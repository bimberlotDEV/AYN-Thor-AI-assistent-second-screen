package com.gameside.data.privacy

import com.gameside.data.database.GameSideDatabase
import com.gameside.data.database.PrivacyDao
import com.gameside.domain.ai.AiCredentialIds
import com.gameside.domain.privacy.LocalDataSummary
import com.gameside.domain.privacy.PrivacyRepository
import com.gameside.domain.security.CredentialStore
import com.gameside.domain.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class RoomPrivacyRepository @Inject constructor(
    private val dao: PrivacyDao,
    private val database: GameSideDatabase,
    private val credentials: CredentialStore,
    private val settings: SettingsRepository,
) : PrivacyRepository {
    private val credentialRefresh = MutableStateFlow(0)
    private val counts = combine(
        dao.gameCount(),
        dao.conversationCount(),
        dao.savedAnswerCount(),
        dao.noteCount(),
        dao.checklistCount(),
        dao.wikiPageCount(),
    ) { values ->
        values
    }
    override val summary: Flow<LocalDataSummary> = combine(counts, credentialRefresh) { values, _ ->
        LocalDataSummary(
            gameProfiles = values[0],
            conversations = values[1],
            savedAnswers = values[2],
            notes = values[3],
            checklists = values[4],
            cachedWikiPages = values[5],
            hasProviderCredential = credentials.contains(AiCredentialIds.DEEPSEEK_API_KEY),
        )
    }

    override suspend fun clearConversations() = dao.clearConversations()
    override suspend fun clearPersonalTools() = dao.clearPersonalTools()
    override suspend fun clearWikiCache() = dao.clearWikiCache()
    override suspend fun removeProviderCredential() {
        credentials.remove(AiCredentialIds.DEEPSEEK_API_KEY)
        credentialRefresh.value += 1
    }

    override suspend fun resetAllData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        credentials.clearAll()
        settings.clearAll()
    }
}
