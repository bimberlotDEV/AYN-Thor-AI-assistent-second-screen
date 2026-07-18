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
    ],
    version = 2,
    exportSchema = true,
)
abstract class GameSideDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
    abstract fun chatDao(): ChatDao
}
