package com.gameside.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.Relation

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @androidx.room.PrimaryKey val id: String,
    val title: String,
    val platform: String,
    val coverImageUri: String?,
    val spoilerLevel: String,
    val currentArea: String?,
    val currentChapter: String?,
    val currentQuest: String?,
    val customContext: String?,
    val customSystemPrompt: String?,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "game_packages",
    primaryKeys = ["gameProfileId", "packageName"],
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
data class GamePackageEntity(
    val gameProfileId: String,
    val packageName: String,
)

@Entity(
    tableName = "game_wiki_sources",
    primaryKeys = ["gameProfileId", "url"],
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
data class GameWikiSourceEntity(
    val gameProfileId: String,
    val url: String,
)

data class GameProfileWithRelations(
    @Embedded val profile: GameProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "gameProfileId")
    val packages: List<GamePackageEntity>,
    @Relation(parentColumn = "id", entityColumn = "gameProfileId")
    val wikiSources: List<GameWikiSourceEntity>,
)
