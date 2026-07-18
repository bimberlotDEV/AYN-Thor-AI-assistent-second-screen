package com.gameside.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = GameProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("gameProfileId")],
)
data class ChatSessionEntity(
    @androidx.room.PrimaryKey val id: String,
    val gameProfileId: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class ChatMessageEntity(
    @androidx.room.PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAtEpochMillis: Long,
)

data class ChatThreadEntity(
    @Embedded val session: ChatSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val messages: List<ChatMessageEntity>,
)
