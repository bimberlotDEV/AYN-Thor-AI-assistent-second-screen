package com.gameside.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "quick_question_favorites",
    foreignKeys = [ForeignKey(
        entity = GameProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["gameProfileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("gameProfileId")],
)
data class QuickQuestionFavoriteEntity(
    @androidx.room.PrimaryKey val id: String,
    val gameProfileId: String,
    val label: String,
    val question: String,
    val category: String,
    val position: Int,
    val createdAtEpochMillis: Long,
)
