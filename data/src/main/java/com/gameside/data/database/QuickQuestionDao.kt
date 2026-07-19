package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickQuestionDao {
    @Query("SELECT * FROM quick_question_favorites WHERE gameProfileId = :gameId ORDER BY position, createdAtEpochMillis")
    fun observe(gameId: String): Flow<List<QuickQuestionFavoriteEntity>>

    @Upsert suspend fun upsert(value: QuickQuestionFavoriteEntity)

    @Query("DELETE FROM quick_question_favorites WHERE id = :id")
    suspend fun delete(id: String)
}
