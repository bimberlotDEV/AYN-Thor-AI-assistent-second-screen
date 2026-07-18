package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PersonalToolsDao {
    @Query("SELECT * FROM saved_answers WHERE gameProfileId = :gameId ORDER BY createdAtEpochMillis DESC")
    abstract fun observeSavedAnswers(gameId: String): Flow<List<SavedAnswerEntity>>

    @Query("SELECT * FROM game_notes WHERE gameProfileId = :gameId ORDER BY updatedAtEpochMillis DESC")
    abstract fun observeNotes(gameId: String): Flow<List<GameNoteEntity>>

    @Transaction
    @Query("SELECT * FROM game_checklists WHERE gameProfileId = :gameId ORDER BY updatedAtEpochMillis DESC")
    abstract fun observeChecklists(gameId: String): Flow<List<ChecklistWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun upsertSavedAnswer(value: SavedAnswerEntity)
    @Query("DELETE FROM saved_answers WHERE id = :id") abstract suspend fun deleteSavedAnswer(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun upsertNote(value: GameNoteEntity)
    @Query("DELETE FROM game_notes WHERE id = :id") abstract suspend fun deleteNote(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun upsertChecklistEntity(value: GameChecklistEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) protected abstract suspend fun upsertItems(values: List<ChecklistItemEntity>)
    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId") protected abstract suspend fun deleteChecklistItems(checklistId: String)
    @Query("UPDATE checklist_items SET isChecked = :checked WHERE id = :itemId") abstract suspend fun setItemChecked(itemId: String, checked: Boolean)
    @Query("DELETE FROM game_checklists WHERE id = :id") abstract suspend fun deleteChecklist(id: String)

    @Transaction
    open suspend fun upsertChecklist(value: GameChecklistEntity, items: List<ChecklistItemEntity>) {
        upsertChecklistEntity(value)
        deleteChecklistItems(value.id)
        if (items.isNotEmpty()) upsertItems(items)
    }
}
