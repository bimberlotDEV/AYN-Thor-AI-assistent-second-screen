package com.gameside.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    override val settings: Flow<AppSettings> = context.gameSideDataStore.data.map { preferences ->
        AppSettings(
            onboardingComplete = preferences[Keys.onboardingComplete] ?: false,
            activeGameId = preferences[Keys.activeGameId],
            trueBlackMode = preferences[Keys.trueBlackMode] ?: true,
            reducedMotion = preferences[Keys.reducedMotion] ?: false,
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
}
