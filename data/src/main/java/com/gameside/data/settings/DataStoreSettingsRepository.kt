package com.gameside.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gameside.domain.settings.AppSettings
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameSideDataStore by preferencesDataStore(name = "gameside_settings")

class DataStoreSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {
    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val activeGameId = stringPreferencesKey("active_game_id")
        val trueBlackMode = booleanPreferencesKey("true_black_mode")
        val reducedMotion = booleanPreferencesKey("reduced_motion")
        val aiModel = stringPreferencesKey("ai_model")
        val maxAnswerTokens = intPreferencesKey("max_answer_tokens")
    }

    override val settings: Flow<AppSettings> = context.gameSideDataStore.data.map { preferences ->
        AppSettings(
            onboardingComplete = preferences[Keys.onboardingComplete] ?: false,
            activeGameId = preferences[Keys.activeGameId],
            trueBlackMode = preferences[Keys.trueBlackMode] ?: true,
            reducedMotion = preferences[Keys.reducedMotion] ?: false,
            aiModel = preferences[Keys.aiModel] ?: "deepseek-v4-flash",
            maxAnswerTokens = preferences[Keys.maxAnswerTokens] ?: 900,
        )
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.gameSideDataStore.edit { it[Keys.onboardingComplete] = complete }
    }

    override suspend fun setActiveGame(id: String?) {
        context.gameSideDataStore.edit { preferences ->
            if (id == null) preferences.remove(Keys.activeGameId) else preferences[Keys.activeGameId] = id
        }
    }

    override suspend fun setTrueBlackMode(enabled: Boolean) {
        context.gameSideDataStore.edit { it[Keys.trueBlackMode] = enabled }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        context.gameSideDataStore.edit { it[Keys.reducedMotion] = enabled }
    }

    override suspend fun setAiModel(model: String) {
        require(model in SUPPORTED_MODELS)
        context.gameSideDataStore.edit { it[Keys.aiModel] = model }
    }

    override suspend fun setMaxAnswerTokens(tokens: Int) {
        require(tokens in 256..4_096)
        context.gameSideDataStore.edit { it[Keys.maxAnswerTokens] = tokens }
    }

    private companion object {
        val SUPPORTED_MODELS = setOf("deepseek-v4-flash", "deepseek-v4-pro")
    }
}
