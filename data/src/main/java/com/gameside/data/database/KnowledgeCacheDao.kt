package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCacheDao {
    @Query("SELECT * FROM knowledge_cache WHERE gameProfileId = :gameId ORDER BY retrievedAtEpochMillis DESC")
    fun observe(gameId: String): Flow<List<KnowledgeCacheEntity>>

    @Query("SELECT * FROM knowledge_cache WHERE gameProfileId = :gameId AND sourceApiUrl = :sourceApiUrl AND pageId = :pageId LIMIT 1")
    suspend fun get(gameId: String, sourceApiUrl: String, pageId: String): KnowledgeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: KnowledgeCacheEntity)

    @Query("DELETE FROM knowledge_cache WHERE gameProfileId = :gameId")
    suspend fun clear(gameId: String)
}

