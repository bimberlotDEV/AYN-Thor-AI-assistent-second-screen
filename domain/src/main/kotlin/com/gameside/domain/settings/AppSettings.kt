package com.gameside.domain.settings

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val activeGameId: String? = null,
    val trueBlackMode: Boolean = true,
    val reducedMotion: Boolean = false,
    val aiModel: String = "deepseek-v4-flash",
    val maxAnswerTokens: Int = 900,
    val controllerShortcutEnabled: Boolean = false,
    val controllerShortcutKeyCode: Int = 108,
    val controllerShortcutLongPressMillis: Int = 800,
    val controllerCalibrationPending: Boolean = false,
)
