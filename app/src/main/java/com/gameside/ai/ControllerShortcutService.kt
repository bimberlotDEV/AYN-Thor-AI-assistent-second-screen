package com.gameside.ai

import android.accessibilityservice.AccessibilityService
import android.view.Display
import android.view.InputDevice
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.gameside.domain.settings.AppSettings
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.gameside.device.ControllerShortcutPolicy
import com.gameside.device.CompanionRestoreTrigger
import com.gameside.device.CompanionSessionCoordinator
import com.gameside.device.PrimaryWindowChanged
import android.os.Build
import android.hardware.display.DisplayManager

@AndroidEntryPoint
class ControllerShortcutService : AccessibilityService() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sessionCoordinator: CompanionSessionCoordinator
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var settings = AppSettings()

    override fun onServiceConnected() {
        serviceScope.launch { settingsRepository.settings.collectLatest { settings = it } }
        sessionCoordinator.accessibilityConnected()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!event.isController()) return false
        val snapshot = settings
        if (snapshot.controllerCalibrationPending && event.action == KeyEvent.ACTION_UP) {
            serviceScope.launch {
                settingsRepository.setControllerShortcutKeyCode(event.keyCode)
                settingsRepository.setControllerCalibrationPending(false)
                settingsRepository.setControllerShortcutEnabled(true)
            }
            return false
        }
        if (ControllerShortcutPolicy.shouldLaunch(
                snapshot.controllerShortcutKeyCode, event.keyCode, event.action == KeyEvent.ACTION_UP,
                event.eventTime, event.downTime, snapshot.controllerShortcutLongPressMillis, snapshot.controllerShortcutEnabled,
            )) launchCompanion()
        return false
    }

    private fun launchCompanion() {
        val secondary = getSystemService(DisplayManager::class.java).displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (secondary != null) {
            if (!sessionCoordinator.state.value.sessionActive) sessionCoordinator.startSession(secondary.displayId)
            sessionCoordinator.requestRestore(CompanionRestoreTrigger.ControllerShortcut)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString()?.takeIf(String::isNotBlank) ?: return
        val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) event.displayId else Display.DEFAULT_DISPLAY
        sessionCoordinator.primaryWindowChanged(PrimaryWindowChanged(packageName, displayId, event.eventTime))
    }
    override fun onInterrupt() = Unit
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }

    private fun KeyEvent.isController(): Boolean = isFromSource(InputDevice.SOURCE_GAMEPAD) ||
        isFromSource(InputDevice.SOURCE_DPAD) || isFromSource(InputDevice.SOURCE_JOYSTICK)
}
