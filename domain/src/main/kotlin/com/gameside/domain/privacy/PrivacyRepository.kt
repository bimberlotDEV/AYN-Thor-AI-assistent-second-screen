package com.gameside.domain.privacy

import kotlinx.coroutines.flow.Flow

data class LocalDataSummary(
    val gameProfiles: Int = 0,
    val conversations: Int = 0,
    val savedAnswers: Int = 0,
    val notes: Int = 0,
    val checklists: Int = 0,
    val cachedWikiPages: Int = 0,
    val hasProviderCredential: Boolean = false,
)

interface PrivacyRepository {
    val summary: Flow<LocalDataSummary>
    suspend fun clearConversations()
    suspend fun clearPersonalTools()
    suspend fun clearWikiCache()
    suspend fun removeProviderCredential()
    suspend fun resetAllData()
}
