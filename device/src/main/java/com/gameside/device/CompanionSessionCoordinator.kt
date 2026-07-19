package com.gameside.device

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.view.Display
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CompanionSessionStatus { inactive, visible, temporarilyDisplaced, restoring, failed }

data class CompanionSessionState(
    val status: CompanionSessionStatus = CompanionSessionStatus.inactive,
    val sessionActive: Boolean = false,
    val keepActive: Boolean = true,
    val targetDisplayId: Int? = null,
    val generation: Long = 0,
    val attemptsLastMinute: Int = 0,
    val lastError: String? = null,
)

data class PrimaryWindowChanged(val packageName: String, val displayId: Int, val timestamp: Long)

enum class CompanionRestoreTrigger { PrimaryWindowChanged, DisplayReturned, AccessibilityConnected, Manual, ControllerShortcut }

sealed interface CompanionRestoreResult {
    data class Requested(val displayId: Int) : CompanionRestoreResult
    data class Failed(val errorType: String) : CompanionRestoreResult
}

fun interface CompanionRestorer {
    fun restoreCompanion(displayId: Int): CompanionRestoreResult
}

internal class RestoreAttemptLimiter(
    private val cooldownMillis: Long = 5_000,
    private val windowMillis: Long = 60_000,
    private val maximumAttempts: Int = 3,
) {
    private val attempts = ArrayDeque<Long>()

    fun replace(values: Iterable<Long>, now: Long) {
        attempts.clear()
        attempts.addAll(values)
        prune(now)
    }

    fun decision(now: Long, manual: Boolean = false): RestoreLimitDecision {
        prune(now)
        if (manual) return RestoreLimitDecision.Allowed
        if (attempts.size >= maximumAttempts) return RestoreLimitDecision.LimitReached
        if (attempts.lastOrNull()?.let { now - it < cooldownMillis } == true) return RestoreLimitDecision.Cooldown
        return RestoreLimitDecision.Allowed
    }

    fun record(now: Long) { prune(now); attempts.addLast(now) }
    fun reset() = attempts.clear()
    fun snapshot(now: Long): List<Long> { prune(now); return attempts.toList() }
    private fun prune(now: Long) { while (attempts.firstOrNull()?.let { now - it >= windowMillis } == true) attempts.removeFirst() }
}

internal enum class RestoreLimitDecision { Allowed, Cooldown, LimitReached }

internal object CompanionWindowPolicy {
    fun isEligiblePrimaryApp(packageName: String, ownPackage: String, homePackages: Set<String>): Boolean {
        if (packageName == ownPackage || packageName in homePackages) return false
        return packageName != "android" &&
            !packageName.startsWith("com.android.systemui") &&
            packageName != "com.android.settings" &&
            !packageName.contains("permissioncontroller", ignoreCase = true) &&
            !packageName.contains("launcher", ignoreCase = true)
    }

    fun selectSecondaryDisplay(preferred: Int?, availableDisplayIds: Collection<Int>): Int? =
        preferred?.takeIf { it != Display.DEFAULT_DISPLAY && it in availableDisplayIds }
            ?: availableDisplayIds.firstOrNull { it != Display.DEFAULT_DISPLAY }
}

internal object CompanionSessionReducer {
    fun start(previous: CompanionSessionState, displayId: Int): CompanionSessionState = previous.copy(
        status = if (previous.sessionActive && previous.status == CompanionSessionStatus.visible && previous.targetDisplayId == displayId) {
            CompanionSessionStatus.visible
        } else CompanionSessionStatus.temporarilyDisplaced,
        sessionActive = true,
        keepActive = true,
        targetDisplayId = displayId,
        generation = previous.generation + 1,
        attemptsLastMinute = 0,
        lastError = null,
    )

    fun visible(previous: CompanionSessionState, displayId: Int): CompanionSessionState = previous.copy(
        status = CompanionSessionStatus.visible,
        sessionActive = true,
        targetDisplayId = displayId,
        lastError = null,
    )

    fun hidden(previous: CompanionSessionState): CompanionSessionState = if (previous.sessionActive) {
        previous.copy(status = CompanionSessionStatus.temporarilyDisplaced)
    } else previous

    fun stop(previous: CompanionSessionState): CompanionSessionState = CompanionSessionState(generation = previous.generation + 1)
}

@Singleton
class CompanionSessionCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val restorer: CompanionRestorer,
) : DisplayManager.DisplayListener {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val limiter = RestoreAttemptLimiter()
    private var pendingRestore: Job? = null
    private val homePackages: Set<String> by lazy(::resolveHomePackages)
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<CompanionSessionState> = _state.asStateFlow()

    init {
        displayManager.registerDisplayListener(this, null)
        limiter.replace(loadAttempts(), System.currentTimeMillis())
        if (_state.value.sessionActive) {
            update(_state.value.copy(status = CompanionSessionStatus.temporarilyDisplaced), "process-restored")
        }
    }

    @Synchronized
    fun startSession(displayId: Int) {
        limiter.reset()
        preferences.edit().remove(KEY_ATTEMPTS).apply()
        update(
            CompanionSessionReducer.start(_state.value, displayId),
            "session-start",
            "display=$displayId",
        )
    }

    @Synchronized
    fun activityVisible(displayId: Int) {
        val current = _state.value
        if (!current.sessionActive) startSession(displayId)
        update(
            CompanionSessionReducer.visible(_state.value, displayId),
            "activity-visible",
            "display=$displayId flags=NEW_TASK|CLEAR_TOP|SINGLE_TOP",
        )
    }

    @Synchronized
    fun activityHidden(reason: String) {
        val current = _state.value
        if (!current.sessionActive) return
        update(CompanionSessionReducer.hidden(current), "activity-hidden", "reason=${safe(reason)}")
    }

    fun recordLifecycle(event: String, displayId: Int, flags: Int = 0) {
        appendDiagnostic("lifecycle-$event", "display=$displayId flags=0x${flags.toString(16)}")
    }

    @Synchronized
    fun setKeepActive(enabled: Boolean) {
        update(_state.value.copy(keepActive = enabled), "keep-active", "enabled=$enabled")
        if (enabled && _state.value.sessionActive && _state.value.status != CompanionSessionStatus.visible) {
            requestRestore(CompanionRestoreTrigger.Manual)
        }
    }

    fun primaryWindowChanged(event: PrimaryWindowChanged) {
        if (event.displayId != Display.DEFAULT_DISPLAY || !isEligiblePrimaryPackage(event.packageName)) return
        appendDiagnostic("primary-window", "display=${event.displayId} packageHash=${event.packageName.hashCode().toUInt().toString(16)}")
        scheduleRestore(CompanionRestoreTrigger.PrimaryWindowChanged, 750)
    }

    fun accessibilityConnected() {
        appendDiagnostic("accessibility-connected")
        val current = _state.value
        if (current.sessionActive && current.keepActive && current.status != CompanionSessionStatus.visible) {
            scheduleRestore(CompanionRestoreTrigger.AccessibilityConnected, 750)
        }
    }

    fun requestRestore(trigger: CompanionRestoreTrigger) = scheduleRestore(trigger, if (trigger == CompanionRestoreTrigger.Manual) 0 else 750)

    @Synchronized
    fun stopSession() {
        pendingRestore?.cancel()
        pendingRestore = null
        limiter.reset()
        update(
            CompanionSessionReducer.stop(_state.value),
            "session-stop",
        )
        preferences.edit().remove(KEY_ATTEMPTS).apply()
    }

    fun diagnosticsText(): String = buildString {
        val snapshot = _state.value
        appendLine("GameSide companion diagnostics")
        appendLine("status=${snapshot.status} active=${snapshot.sessionActive} keepActive=${snapshot.keepActive}")
        appendLine("targetDisplay=${snapshot.targetDisplayId ?: "none"} generation=${snapshot.generation} attempts=${snapshot.attemptsLastMinute}")
        appendLine("availableDisplays=${displayManager.displays.joinToString { "${it.displayId}:${displayCapabilities(it)}" }}")
        snapshot.lastError?.let { appendLine("lastError=$it") }
        appendLine("events:")
        preferences.getString(KEY_DIAGNOSTICS, "").orEmpty().lineSequence().filter(String::isNotBlank).forEach { appendLine(it) }
    }

    override fun onDisplayAdded(displayId: Int) {
        appendDiagnostic("display-added", "display=$displayId")
        val current = _state.value
        if (current.sessionActive && current.keepActive && displayId != Display.DEFAULT_DISPLAY) {
            if (current.targetDisplayId == null || !isValidSecondaryDisplay(current.targetDisplayId)) {
                update(current.copy(targetDisplayId = displayId, status = CompanionSessionStatus.temporarilyDisplaced), "target-display-updated", "display=$displayId")
            }
            scheduleRestore(CompanionRestoreTrigger.DisplayReturned, 750)
        }
    }

    override fun onDisplayRemoved(displayId: Int) {
        appendDiagnostic("display-removed", "display=$displayId")
        val current = _state.value
        if (current.sessionActive && current.targetDisplayId == displayId) {
            update(current.copy(status = CompanionSessionStatus.temporarilyDisplaced, lastError = "Target display temporarily unavailable"), "target-display-missing")
        }
    }

    override fun onDisplayChanged(displayId: Int) {
        appendDiagnostic("display-changed", "display=$displayId")
    }

    private fun scheduleRestore(trigger: CompanionRestoreTrigger, delayMillis: Long) {
        val generation = _state.value.generation
        pendingRestore?.cancel()
        pendingRestore = scope.launch {
            delay(delayMillis)
            performRestore(trigger, generation)
        }
    }

    @Synchronized
    private fun performRestore(trigger: CompanionRestoreTrigger, expectedGeneration: Long) {
        val current = _state.value
        val manual = trigger == CompanionRestoreTrigger.Manual || trigger == CompanionRestoreTrigger.ControllerShortcut
        if (!current.sessionActive || current.generation != expectedGeneration) return
        if (!manual && (!current.keepActive || current.status == CompanionSessionStatus.visible)) return
        val displayId = validTarget(current.targetDisplayId)
        if (displayId == null) {
            update(current.copy(status = CompanionSessionStatus.temporarilyDisplaced, lastError = "No secondary display is currently available"), "restore-wait-display", "trigger=$trigger")
            return
        }
        val now = System.currentTimeMillis()
        when (limiter.decision(now, manual)) {
            RestoreLimitDecision.Cooldown -> {
                appendDiagnostic("restore-skipped", "reason=cooldown trigger=$trigger")
                return
            }
            RestoreLimitDecision.LimitReached -> {
                update(current.copy(status = CompanionSessionStatus.failed, attemptsLastMinute = 3, lastError = "Automatic restore limit reached"), "restore-failed", "reason=limit")
                return
            }
            RestoreLimitDecision.Allowed -> Unit
        }
        limiter.record(now)
        saveAttempts(now)
        val nextGeneration = current.generation + 1
        update(current.copy(status = CompanionSessionStatus.restoring, generation = nextGeneration, attemptsLastMinute = limiter.snapshot(now).size, lastError = null), "restore-attempt", "trigger=$trigger display=$displayId")
        when (val result = restorer.restoreCompanion(displayId)) {
            is CompanionRestoreResult.Requested -> {
                appendDiagnostic("restore-requested", "display=${result.displayId} generation=$nextGeneration")
                scope.launch {
                    delay(2_500)
                    val latest = _state.value
                    if (latest.generation == nextGeneration && latest.status == CompanionSessionStatus.restoring) {
                        update(latest.copy(status = CompanionSessionStatus.failed, lastError = "Companion did not become visible"), "restore-timeout")
                    }
                }
            }
            is CompanionRestoreResult.Failed -> update(
                _state.value.copy(status = CompanionSessionStatus.failed, lastError = result.errorType),
                "restore-failed",
                "error=${safe(result.errorType)}",
            )
        }
    }

    private fun validTarget(preferred: Int?): Int? = CompanionWindowPolicy.selectSecondaryDisplay(
        preferred,
        displayManager.displays.map { it.displayId },
    )

    private fun isValidSecondaryDisplay(displayId: Int): Boolean = displayId != Display.DEFAULT_DISPLAY && displayManager.getDisplay(displayId) != null

    private fun isEligiblePrimaryPackage(packageName: String): Boolean {
        return CompanionWindowPolicy.isEligiblePrimaryApp(packageName, context.packageName, homePackages)
    }

    private fun resolveHomePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    private fun loadState(): CompanionSessionState {
        val active = preferences.getBoolean(KEY_ACTIVE, false)
        val savedAt = preferences.getLong(KEY_SAVED_AT, 0)
        val elapsedAt = preferences.getLong(KEY_ELAPSED_AT, 0)
        val rebooted = elapsedAt > SystemClock.elapsedRealtime()
        val stale = System.currentTimeMillis() - savedAt > SESSION_MAX_AGE
        if (active && (rebooted || stale)) preferences.edit().putBoolean(KEY_ACTIVE, false).apply()
        val validActive = active && !rebooted && !stale
        return CompanionSessionState(
            status = if (validActive) CompanionSessionStatus.temporarilyDisplaced else CompanionSessionStatus.inactive,
            sessionActive = validActive,
            keepActive = preferences.getBoolean(KEY_KEEP_ACTIVE, true),
            targetDisplayId = preferences.getInt(KEY_TARGET_DISPLAY, Int.MIN_VALUE).takeUnless { it == Int.MIN_VALUE },
            generation = preferences.getLong(KEY_GENERATION, 0),
            lastError = preferences.getString(KEY_LAST_ERROR, null),
        )
    }

    @Synchronized
    private fun update(next: CompanionSessionState, event: String, details: String = "") {
        _state.value = next
        preferences.edit()
            .putBoolean(KEY_ACTIVE, next.sessionActive)
            .putBoolean(KEY_KEEP_ACTIVE, next.keepActive)
            .putLong(KEY_GENERATION, next.generation)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .putLong(KEY_ELAPSED_AT, SystemClock.elapsedRealtime())
            .apply {
                if (next.targetDisplayId == null) remove(KEY_TARGET_DISPLAY) else putInt(KEY_TARGET_DISPLAY, next.targetDisplayId)
                if (next.lastError == null) remove(KEY_LAST_ERROR) else putString(KEY_LAST_ERROR, next.lastError)
            }
            .apply()
        appendDiagnostic(event, details)
    }

    private fun loadAttempts(): List<Long> = preferences.getString(KEY_ATTEMPTS, "").orEmpty()
        .split(',').mapNotNull(String::toLongOrNull)

    private fun saveAttempts(now: Long) {
        preferences.edit().putString(KEY_ATTEMPTS, limiter.snapshot(now).joinToString(",")).apply()
    }

    @Synchronized
    private fun appendDiagnostic(event: String, details: String = "") {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val line = listOf(formatter.format(Date()), safe(event), safe(details)).filter(String::isNotBlank).joinToString(" | ")
        val existing = preferences.getString(KEY_DIAGNOSTICS, "").orEmpty().lineSequence().filter(String::isNotBlank).toMutableList()
        existing += line
        preferences.edit().putString(KEY_DIAGNOSTICS, existing.takeLast(50).joinToString("\n")).apply()
    }

    private fun displayCapabilities(display: Display): String = buildList {
        add(if (display.displayId == Display.DEFAULT_DISPLAY) "primary" else "secondary")
        if (display.flags and Display.FLAG_PRIVATE != 0) add("private")
        if (display.flags and Display.FLAG_PRESENTATION != 0) add("presentation")
        if (display.flags and Display.FLAG_SECURE != 0) add("secure")
    }.joinToString("+")

    private fun safe(value: String): String = value.replace(Regex("[\\r\\n|]"), " ").take(160)

    private companion object {
        const val PREFERENCES = "companion_session"
        const val KEY_ACTIVE = "active"
        const val KEY_KEEP_ACTIVE = "keep_active"
        const val KEY_TARGET_DISPLAY = "target_display"
        const val KEY_GENERATION = "generation"
        const val KEY_LAST_ERROR = "last_error"
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_DIAGNOSTICS = "diagnostics"
        const val KEY_SAVED_AT = "saved_at"
        const val KEY_ELAPSED_AT = "elapsed_at"
        const val SESSION_MAX_AGE = 12 * 60 * 60 * 1_000L
    }
}
