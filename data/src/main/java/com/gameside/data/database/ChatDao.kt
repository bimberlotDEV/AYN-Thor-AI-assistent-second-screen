package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChatDao {
    @Transaction
    @Query("SELECT * FROM chat_sessions WHERE gameProfileId = :gameId ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    abstract fun observeLatestThread(gameId: String): Flow<ChatThreadEntity?>

    @Query("SELECT * FROM chat_sessions WHERE gameProfileId = :gameId ORDER BY updatedAtEpochMillis DESC LIMIT 1")
    abstract suspend fun latestSession(gameId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMessageEntity(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertCitationEntities(citations: List<SourceCitationEntity>)

    @Query("UPDATE chat_sessions SET updatedAtEpochMillis = :updatedAt WHERE id = :sessionId")
    protected abstract suspend fun touchSession(sessionId: String, updatedAt: Long)

    @Transaction
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtEpochMillis DESC LIMIT :limit")
    abstract suspend fun recentMessages(sessionId: String, limit: Int): List<ChatMessageWithCitations>

    @Query("DELETE FROM chat_sessions WHERE gameProfileId = :gameId")
    abstract suspend fun clearGameHistory(gameId: String)

    @Transaction
    open suspend fun insertMessage(message: ChatMessageWithCitations) {
        insertMessageEntity(message.message)
        if (message.citations.isNotEmpty()) insertCitationEntities(message.citations)
        touchSession(message.message.sessionId, message.message.createdAtEpochMillis)
    }
}
