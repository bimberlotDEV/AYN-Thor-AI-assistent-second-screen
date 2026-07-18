package com.gameside.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setActiveGame(id: String?)
    suspend fun setTrueBlackMode(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setAiModel(model: String)
    suspend fun setMaxAnswerTokens(tokens: Int)
    suspend fun clearAll()
}
