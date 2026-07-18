package com.gameside.data.chat

import com.gameside.data.database.ChatDao
import com.gameside.data.database.ChatMessageEntity
import com.gameside.data.database.ChatSessionEntity
import com.gameside.data.database.ChatThreadEntity
import com.gameside.domain.chat.ChatMessage
import com.gameside.domain.chat.ChatRepository
import com.gameside.domain.chat.ChatRole
import com.gameside.domain.chat.ChatSession
import com.gameside.domain.chat.ChatThread
import com.gameside.domain.game.GameProfile
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatRepository @Inject constructor(private val dao: ChatDao) : ChatRepository {
    override fun observeLatestThread(gameProfileId: String): Flow<ChatThread?> =
        dao.observeLatestThread(gameProfileId).map { it?.toDomain() }

    override suspend fun getOrCreateSession(game: GameProfile): ChatSession {
        dao.latestSession(game.id)?.let { return it.toDomain() }
        val now = Instant.now()
        val session = ChatSessionEntity(UUID.randomUUID().toString(), game.id, game.title, now.toEpochMilli(), now.toEpochMilli())
        dao.insertSession(session)
        return session.toDomain()
    }

    override suspend fun addMessage(message: ChatMessage) {
        dao.insertMessage(message.toEntity())
    }

    override suspend fun recentMessages(sessionId: String, limit: Int): List<ChatMessage> =
        dao.recentMessages(sessionId, limit).asReversed().map { it.toDomain() }

    override suspend fun clearGameHistory(gameProfileId: String) = dao.clearGameHistory(gameProfileId)

    private fun ChatThreadEntity.toDomain() = ChatThread(
        session.toDomain(),
        messages.sortedBy { it.createdAtEpochMillis }.map { it.toDomain() },
    )

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id, gameProfileId, title, Instant.ofEpochMilli(createdAtEpochMillis), Instant.ofEpochMilli(updatedAtEpochMillis),
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id, sessionId, ChatRole.valueOf(role), content, Instant.ofEpochMilli(createdAtEpochMillis),
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(id, sessionId, role.name, content, createdAt.toEpochMilli())
}
