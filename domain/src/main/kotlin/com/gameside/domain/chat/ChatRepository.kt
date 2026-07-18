package com.gameside.domain.chat

import com.gameside.domain.game.GameProfile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeLatestThread(gameProfileId: String): Flow<ChatThread?>
    suspend fun getOrCreateSession(game: GameProfile): ChatSession
    suspend fun addMessage(message: ChatMessage)
    suspend fun recentMessages(sessionId: String, limit: Int): List<ChatMessage>
    suspend fun clearGameHistory(gameProfileId: String)
}
