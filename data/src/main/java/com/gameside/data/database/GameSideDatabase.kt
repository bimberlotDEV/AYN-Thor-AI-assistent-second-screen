package com.gameside.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameProfileEntity::class,
        GamePackageEntity::class,
        GameWikiSourceEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SourceCitationEntity::class,
        SavedAnswerEntity::class,
        GameNoteEntity::class,
        GameChecklistEntity::class,
        ChecklistItemEntity::class,
        KnowledgeCacheEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class GameSideDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun chatDao(): ChatDao
    abstract fun personalToolsDao(): PersonalToolsDao
    abstract fun knowledgeCacheDao(): KnowledgeCacheDao
    abstract fun privacyDao(): PrivacyDao
}
