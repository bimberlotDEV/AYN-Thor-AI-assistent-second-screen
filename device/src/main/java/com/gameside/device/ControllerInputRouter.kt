package com.gameside.device

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class ControllerCommand { OPEN_QUICK, OPEN_KEYWORD, PREVIOUS_TAB, NEXT_TAB }

@Singleton
class ControllerInputRouter @Inject constructor() {
    private val mutableCommands = MutableSharedFlow<ControllerCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<ControllerCommand> = mutableCommands.asSharedFlow()
    private var lastAxisCommandAt = 0L
    private var horizontalLatched = false
    private var verticalLatched = false

    fun onKeyEvent(event: KeyEvent): ControllerCommand? {
        if (event.action != KeyEvent.ACTION_DOWN) return null
        val command = commandFor(event.keyCode, event.repeatCount, event.isController())
        command?.let(mutableCommands::tryEmit)
        return command
    }

    fun isCommandKey(event: KeyEvent): Boolean = event.isController() && event.keyCode in COMMAND_KEY_CODES

    internal fun commandFor(keyCode: Int, repeatCount: Int, isController: Boolean): ControllerCommand? {
        if (repeatCount != 0 || !isController) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_X -> ControllerCommand.OPEN_QUICK
            KeyEvent.KEYCODE_BUTTON_Y -> ControllerCommand.OPEN_KEYWORD
            KeyEvent.KEYCODE_BUTTON_L1 -> ControllerCommand.PREVIOUS_TAB
            KeyEvent.KEYCODE_BUTTON_R1 -> ControllerCommand.NEXT_TAB
            else -> null
        }
    }

    fun onMotionEvent(event: MotionEvent): KeyEvent? {
        if (event.action != MotionEvent.ACTION_MOVE || !event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return null
        val x = event.getAxisValue(MotionEvent.AXIS_HAT_X).takeIf { kotlin.math.abs(it) > DEAD_ZONE }
            ?: event.getAxisValue(MotionEvent.AXIS_X).takeIf { kotlin.math.abs(it) > DEAD_ZONE }
        val y = event.getAxisValue(MotionEvent.AXIS_HAT_Y).takeIf { kotlin.math.abs(it) > DEAD_ZONE }
            ?: event.getAxisValue(MotionEvent.AXIS_Y).takeIf { kotlin.math.abs(it) > DEAD_ZONE }
        if (x == null) horizontalLatched = false
        if (y == null) verticalLatched = false
        val now = event.eventTime
        if (now - lastAxisCommandAt < REPEAT_MILLIS) return null
        val keyCode = when {
            x != null && !horizontalLatched -> if (x < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
            y != null && !verticalLatched -> if (y < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
            else -> return null
        }
        horizontalLatched = x != null
        verticalLatched = y != null
        lastAxisCommandAt = now
        return KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, event.deviceId, 0, 0, InputDevice.SOURCE_DPAD)
    }

    private fun KeyEvent.isController(): Boolean = isFromSource(InputDevice.SOURCE_GAMEPAD) ||
        isFromSource(InputDevice.SOURCE_DPAD) || isFromSource(InputDevice.SOURCE_JOYSTICK)

    private companion object {
        const val DEAD_ZONE = 0.65f
        const val REPEAT_MILLIS = 180L
        val COMMAND_KEY_CODES = setOf(
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        )
    }
}
