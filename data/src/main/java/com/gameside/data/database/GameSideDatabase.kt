package com.gameside.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameProfileEntity::class, GamePackageEntity::class, GameWikiSourceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GameSideDatabase : RoomDatabase() {
    abstract fun gameProfileDao(): GameProfileDao
}
