package com.gameside.domain.chat

import com.gameside.domain.game.GameProfile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeLatestThread(gameProfileId: String): Flow<ChatThread?>
    fun observeSessions(gameProfileId: String): Flow<List<ChatSession>>
    fun observeThread(sessionId: String): Flow<ChatThread?>
    suspend fun getOrCreateSession(game: GameProfile): ChatSession
    suspend fun createSession(game: GameProfile): ChatSession
    suspend fun renameSession(sessionId: String, title: String)
    suspend fun deleteSession(sessionId: String)
    suspend fun addMessage(message: ChatMessage)
    suspend fun recentMessages(sessionId: String, limit: Int): List<ChatMessage>
    suspend fun clearGameHistory(gameProfileId: String)
}
