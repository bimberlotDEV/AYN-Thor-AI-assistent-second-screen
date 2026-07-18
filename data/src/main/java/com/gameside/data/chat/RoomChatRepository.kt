package com.gameside.data.chat

import com.gameside.data.database.ChatDao
import com.gameside.data.database.ChatMessageEntity
import com.gameside.data.database.ChatMessageWithCitations
import com.gameside.data.database.ChatSessionEntity
import com.gameside.data.database.ChatThreadEntity
import com.gameside.data.database.SourceCitationEntity
import com.gameside.domain.chat.ChatMessage
import com.gameside.domain.chat.ChatRepository
import com.gameside.domain.chat.ChatRole
import com.gameside.domain.chat.ChatSession
import com.gameside.domain.chat.ChatThread
import com.gameside.domain.game.GameProfile
import com.gameside.domain.knowledge.SourceCitation
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
        messages.sortedBy { it.message.createdAtEpochMillis }.map { it.toDomain() },
    )

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id, gameProfileId, title, Instant.ofEpochMilli(createdAtEpochMillis), Instant.ofEpochMilli(updatedAtEpochMillis),
    )

    private fun ChatMessageWithCitations.toDomain() = ChatMessage(
        message.id, message.sessionId, ChatRole.valueOf(message.role), message.content,
        Instant.ofEpochMilli(message.createdAtEpochMillis), citations.sortedBy { it.position }.map { it.toDomain() },
    )

    private fun SourceCitationEntity.toDomain() = SourceCitation(
        title, sourceName, url, excerpt, Instant.ofEpochMilli(retrievedAtEpochMillis),
    )

    private fun ChatMessage.toEntity() = ChatMessageWithCitations(
        ChatMessageEntity(id, sessionId, role.name, content, createdAt.toEpochMilli()),
        citations.mapIndexed { index, citation ->
            SourceCitationEntity(
                messageId = id, position = index, title = citation.title, sourceName = citation.sourceName,
                url = citation.url, excerpt = citation.excerpt, retrievedAtEpochMillis = citation.retrievedAt.toEpochMilli(),
            )
        },
    )
}
