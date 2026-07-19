package com.gameside.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameside.core.design.GameSideTheme
import com.gameside.device.SecondaryDisplayLauncher
import com.gameside.device.GameLauncher
import com.gameside.features.home.GameSideHomeRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.gameside.device.ControllerInputRouter
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.view.Display

@AndroidEntryPoint
@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {
    @Inject lateinit var displayLauncher: SecondaryDisplayLauncher
    @Inject lateinit var gameLauncher: GameLauncher
    @Inject lateinit var controllerInput: ControllerInputRouter
    private var contentRendered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeToLowerOrRender()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeToLowerOrRender()
    }

    private fun routeToLowerOrRender() {
        val lowerDisplayId = displayLauncher.lowerDisplayFor(this)
        if (lowerDisplayId != null) {
            val result = displayLauncher.launch(this, Intent(this, CompanionActivity::class.java), lowerDisplayId)
            if (result is com.gameside.device.CompanionLaunchResult.Success) {
                // Keep this invisible primary task alive in the background. Thor is more stable when
                // a primary task already exists before the lower-screen activity becomes active.
                if (!moveTaskToBack(true)) finish()
                return
            }
        }
        if (displayLauncher.isOnSecondaryDisplay(this)) ensurePrimaryAnchor()
        if (contentRendered) return
        contentRendered = true
        enableEdgeToEdge()
        setContent {
            GameSideTheme {
                GameSideHomeRoute(
                    onLaunchGame = { packageName -> gameLauncher.launchOnPrimary(this, packageName) },
                )
            }
        }
    }

    private fun ensurePrimaryAnchor() {
        val anchor = Intent(this, PrimaryAnchorActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        runCatching {
            startActivity(anchor, ActivityOptions.makeBasic().apply { launchDisplayId = Display.DEFAULT_DISPLAY }.toBundle())
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

}
