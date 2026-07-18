package com.gameside.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

@Entity(
    tableName = "saved_answers",
    foreignKeys = [ForeignKey(entity = GameProfileEntity::class, parentColumns = ["id"], childColumns = ["gameProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameProfileId"), Index(value = ["sourceMessageId"], unique = true)],
)
data class SavedAnswerEntity(
    @androidx.room.PrimaryKey val id: String,
    val gameProfileId: String,
    val sourceMessageId: String,
    val question: String,
    val answer: String,
    val citationsJson: String,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "game_notes",
    foreignKeys = [ForeignKey(entity = GameProfileEntity::class, parentColumns = ["id"], childColumns = ["gameProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameProfileId")],
)
data class GameNoteEntity(
    @androidx.room.PrimaryKey val id: String,
    val gameProfileId: String,
    val title: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "game_checklists",
    foreignKeys = [ForeignKey(entity = GameProfileEntity::class, parentColumns = ["id"], childColumns = ["gameProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameProfileId")],
)
data class GameChecklistEntity(
    @androidx.room.PrimaryKey val id: String,
    val gameProfileId: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = GameChecklistEntity::class, parentColumns = ["id"], childColumns = ["checklistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("checklistId")],
)
data class ChecklistItemEntity(
    @androidx.room.PrimaryKey val id: String,
    val checklistId: String,
    val text: String,
    val isChecked: Boolean,
    val position: Int,
)

data class ChecklistWithItems(
    @Embedded val checklist: GameChecklistEntity,
    @Relation(parentColumn = "id", entityColumn = "checklistId") val items: List<ChecklistItemEntity>,
)

