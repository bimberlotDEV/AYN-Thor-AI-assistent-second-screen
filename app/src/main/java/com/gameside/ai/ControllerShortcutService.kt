package com.gameside.ai

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
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

@AndroidEntryPoint
class ControllerShortcutService : AccessibilityService() {
    @Inject lateinit var settingsRepository: SettingsRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var settings = AppSettings()

    override fun onServiceConnected() {
        serviceScope.launch { settingsRepository.settings.collectLatest { settings = it } }
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
        val intent = Intent(this, CompanionActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
        )
        val secondary = getSystemService(DisplayManager::class.java).displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        runCatching {
            if (secondary == null) startActivity(intent)
            else startActivity(intent, ActivityOptions.makeBasic().apply { launchDisplayId = secondary.displayId }.toBundle())
        }.recoverCatching { startActivity(intent) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }

    private fun KeyEvent.isController(): Boolean = isFromSource(InputDevice.SOURCE_GAMEPAD) ||
        isFromSource(InputDevice.SOURCE_DPAD) || isFromSource(InputDevice.SOURCE_JOYSTICK)
}
