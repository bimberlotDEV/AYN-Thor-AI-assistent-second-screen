package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PrivacyDao {
    @Query("SELECT COUNT(*) FROM game_profiles") abstract fun gameCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM chat_sessions") abstract fun conversationCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM saved_answers") abstract fun savedAnswerCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM game_notes") abstract fun noteCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM game_checklists") abstract fun checklistCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM knowledge_cache") abstract fun wikiPageCount(): Flow<Int>

    @Query("DELETE FROM chat_sessions") abstract suspend fun clearConversations()
    @Query("DELETE FROM saved_answers") protected abstract suspend fun clearSavedAnswers()
    @Query("DELETE FROM game_notes") protected abstract suspend fun clearNotes()
    @Query("DELETE FROM game_checklists") protected abstract suspend fun clearChecklists()
    @Query("DELETE FROM knowledge_cache") abstract suspend fun clearWikiCache()

    @Transaction
    open suspend fun clearPersonalTools() {
        clearSavedAnswers()
        clearNotes()
        clearChecklists()
    }
}
