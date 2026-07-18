package com.gameside.domain.personal

import com.gameside.domain.knowledge.SourceCitation
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class SavedAnswer(
    val id: String,
    val gameProfileId: String,
    val sourceMessageId: String,
    val question: String,
    val answer: String,
    val citations: List<SourceCitation>,
    val createdAt: Instant,
)

data class GameNote(
    val id: String,
    val gameProfileId: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ChecklistItem(val id: String, val text: String, val isChecked: Boolean, val position: Int)

data class GameChecklist(
    val id: String,
    val gameProfileId: String,
    val title: String,
    val items: List<ChecklistItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PersonalTools(
    val savedAnswers: List<SavedAnswer> = emptyList(),
    val notes: List<GameNote> = emptyList(),
    val checklists: List<GameChecklist> = emptyList(),
)

interface PersonalToolsRepository {
    fun observe(gameProfileId: String): Flow<PersonalTools>
    suspend fun saveAnswer(answer: SavedAnswer)
    suspend fun deleteSavedAnswer(id: String)
    suspend fun saveNote(note: GameNote)
    suspend fun deleteNote(id: String)
    suspend fun saveChecklist(checklist: GameChecklist)
    suspend fun setChecklistItemChecked(itemId: String, checked: Boolean)
    suspend fun deleteChecklist(id: String)
}

