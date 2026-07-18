package com.gameside.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
abstract class BackupDao {
    @Query("SELECT * FROM game_profiles") abstract suspend fun games(): List<GameProfileEntity>
    @Query("SELECT * FROM game_packages") abstract suspend fun packages(): List<GamePackageEntity>
    @Query("SELECT * FROM game_wiki_sources") abstract suspend fun wikiSources(): List<GameWikiSourceEntity>
    @Query("SELECT * FROM chat_sessions") abstract suspend fun sessions(): List<ChatSessionEntity>
    @Query("SELECT * FROM chat_messages") abstract suspend fun messages(): List<ChatMessageEntity>
    @Query("SELECT * FROM source_citations") abstract suspend fun citations(): List<SourceCitationEntity>
    @Query("SELECT * FROM saved_answers") abstract suspend fun savedAnswers(): List<SavedAnswerEntity>
    @Query("SELECT * FROM game_notes") abstract suspend fun notes(): List<GameNoteEntity>
    @Query("SELECT * FROM game_checklists") abstract suspend fun checklists(): List<GameChecklistEntity>
    @Query("SELECT * FROM checklist_items") abstract suspend fun checklistItems(): List<ChecklistItemEntity>

    @Upsert protected abstract suspend fun upsertGames(values: List<GameProfileEntity>)
    @Upsert protected abstract suspend fun upsertPackages(values: List<GamePackageEntity>)
    @Upsert protected abstract suspend fun upsertWikiSources(values: List<GameWikiSourceEntity>)
    @Upsert protected abstract suspend fun upsertSessions(values: List<ChatSessionEntity>)
    @Upsert protected abstract suspend fun upsertMessages(values: List<ChatMessageEntity>)
    @Upsert protected abstract suspend fun upsertCitations(values: List<SourceCitationEntity>)
    @Upsert protected abstract suspend fun upsertSavedAnswers(values: List<SavedAnswerEntity>)
    @Upsert protected abstract suspend fun upsertNotes(values: List<GameNoteEntity>)
    @Upsert protected abstract suspend fun upsertChecklists(values: List<GameChecklistEntity>)
    @Upsert protected abstract suspend fun upsertChecklistItems(values: List<ChecklistItemEntity>)

    @Transaction
    open suspend fun importData(data: BackupData) {
        upsertGames(data.games)
        upsertPackages(data.packages)
        upsertWikiSources(data.wikiSources)
        upsertSessions(data.sessions)
        upsertMessages(data.messages)
        upsertCitations(data.citations)
        upsertSavedAnswers(data.savedAnswers)
        upsertNotes(data.notes)
        upsertChecklists(data.checklists)
        upsertChecklistItems(data.checklistItems)
    }
}

data class BackupData(
    val games: List<GameProfileEntity>,
    val packages: List<GamePackageEntity>,
    val wikiSources: List<GameWikiSourceEntity>,
    val sessions: List<ChatSessionEntity>,
    val messages: List<ChatMessageEntity>,
    val citations: List<SourceCitationEntity>,
    val savedAnswers: List<SavedAnswerEntity>,
    val notes: List<GameNoteEntity>,
    val checklists: List<GameChecklistEntity>,
    val checklistItems: List<ChecklistItemEntity>,
)
