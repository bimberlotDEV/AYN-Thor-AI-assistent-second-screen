package com.gameside.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "knowledge_cache",
    primaryKeys = ["gameProfileId", "sourceApiUrl", "pageId"],
    foreignKeys = [ForeignKey(entity = GameProfileEntity::class, parentColumns = ["id"], childColumns = ["gameProfileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameProfileId")],
)
data class KnowledgeCacheEntity(
    val gameProfileId: String,
    val sourceApiUrl: String,
    val pageId: String,
    val title: String,
    val sourceName: String,
    val url: String,
    val plainText: String,
    val retrievedAtEpochMillis: Long,
)

