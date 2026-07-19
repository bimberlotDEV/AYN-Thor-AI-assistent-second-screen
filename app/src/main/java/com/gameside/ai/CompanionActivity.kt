package com.gameside.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameside.core.design.GameSideTheme
import com.gameside.device.CompanionLaunchResult
import com.gameside.device.SecondaryDisplayLauncher
import com.gameside.device.GameLauncher
import com.gameside.features.home.GameSideHomeRoute
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.gameside.device.ControllerInputRouter
import com.gameside.device.CompanionSessionCoordinator
import android.annotation.SuppressLint
import android.os.Build
import android.view.Display

@AndroidEntryPoint
@SuppressLint("RestrictedApi")
class CompanionActivity : ComponentActivity() {
    @Inject lateinit var displayLauncher: SecondaryDisplayLauncher
    @Inject lateinit var gameLauncher: GameLauncher
    @Inject lateinit var controllerInput: ControllerInputRouter
    @Inject lateinit var sessionCoordinator: CompanionSessionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSideTheme {
                GameSideHomeRoute(
                    onLaunchCompanion = { displayId -> launchCompanion(displayId) },
                    onOpenSingleScreen = { launchCompanion(null) },
                    onLaunchGame = { packageName -> gameLauncher.launchOnPrimary(this, packageName) },
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (controllerInput.onKeyEvent(event) != null || controllerInput.isCommandKey(event)) return true
        if (event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            if (event.keyCode == KeyEvent.KEYCODE_BUTTON_A) return super.dispatchKeyEvent(event.withKeyCode(KeyEvent.KEYCODE_DPAD_CENTER))
            if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B) return super.dispatchKeyEvent(event.withKeyCode(KeyEvent.KEYCODE_BACK))
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        controllerInput.onMotionEvent(event)?.let { return super.dispatchKeyEvent(it) }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun KeyEvent.withKeyCode(keyCode: Int) = KeyEvent(downTime, eventTime, action, keyCode, repeatCount, metaState, deviceId, scanCode, flags, source)

    override fun onResume() {
        super.onResume()
        val displayId = currentDisplayId()
        sessionCoordinator.activityVisible(displayId)
        sessionCoordinator.recordLifecycle("resume", displayId, intent.flags)
    }

    override fun onStop() {
        sessionCoordinator.recordLifecycle("stop", currentDisplayId(), intent.flags)
        if (!isChangingConfigurations) sessionCoordinator.activityHidden("onStop")
        super.onStop()
    }

    override fun onDestroy() {
        sessionCoordinator.recordLifecycle("destroy", currentDisplayId(), intent.flags)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sessionCoordinator.recordLifecycle("new-intent", currentDisplayId(), intent.flags)
    }

    private fun launchCompanion(displayId: Int?): CompanionLaunchResult {
        val intent = Intent(this, CompanionActivity::class.java)
        val result = displayLauncher.launch(this, intent, displayId)
        if (result is CompanionLaunchResult.Success) sessionCoordinator.startSession(result.displayId)
        return result
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayId(): Int = if (Build.VERSION.SDK_INT >= 30) display?.displayId ?: Display.DEFAULT_DISPLAY
    else windowManager.defaultDisplay.displayId
}
