package com.gameside.device

object ControllerShortcutPolicy {
    fun shouldLaunch(
        configuredKeyCode: Int,
        eventKeyCode: Int,
        actionUp: Boolean,
        eventTime: Long,
        downTime: Long,
        thresholdMillis: Int,
        enabled: Boolean,
    ): Boolean = enabled && actionUp && configuredKeyCode == eventKeyCode &&
        eventTime >= downTime && eventTime - downTime >= thresholdMillis
}
