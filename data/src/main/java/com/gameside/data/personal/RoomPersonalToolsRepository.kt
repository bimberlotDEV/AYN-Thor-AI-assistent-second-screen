package com.gameside.data.personal

import com.gameside.data.database.ChecklistItemEntity
import com.gameside.data.database.ChecklistWithItems
import com.gameside.data.database.GameChecklistEntity
import com.gameside.data.database.GameNoteEntity
import com.gameside.data.database.PersonalToolsDao
import com.gameside.data.database.SavedAnswerEntity
import com.gameside.domain.knowledge.SourceCitation
import com.gameside.domain.personal.ChecklistItem
import com.gameside.domain.personal.GameChecklist
import com.gameside.domain.personal.GameNote
import com.gameside.domain.personal.PersonalTools
import com.gameside.domain.personal.PersonalToolsRepository
import com.gameside.domain.personal.SavedAnswer
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject

class RoomPersonalToolsRepository @Inject constructor(private val dao: PersonalToolsDao) : PersonalToolsRepository {
    override fun observe(gameProfileId: String): Flow<PersonalTools> = combine(
        dao.observeSavedAnswers(gameProfileId), dao.observeNotes(gameProfileId), dao.observeChecklists(gameProfileId),
    ) { saved, notes, checklists ->
        PersonalTools(saved.map { it.toDomain() }, notes.map { it.toDomain() }, checklists.map { it.toDomain() })
    }

    override suspend fun saveAnswer(answer: SavedAnswer) = dao.upsertSavedAnswer(answer.toEntity())
    override suspend fun deleteSavedAnswer(id: String) = dao.deleteSavedAnswer(id)
    override suspend fun saveNote(note: GameNote) = dao.upsertNote(note.toEntity())
    override suspend fun deleteNote(id: String) = dao.deleteNote(id)
    override suspend fun saveChecklist(checklist: GameChecklist) = dao.upsertChecklist(
        checklist.toEntity(), checklist.items.map { it.toEntity(checklist.id) },
    )
    override suspend fun setChecklistItemChecked(itemId: String, checked: Boolean) = dao.setItemChecked(itemId, checked)
    override suspend fun deleteChecklist(id: String) = dao.deleteChecklist(id)

    private fun SavedAnswer.toEntity() = SavedAnswerEntity(
        id, gameProfileId, sourceMessageId, question, answer,
        JSONArray().apply { citations.forEach { put(it.toJson()) } }.toString(), createdAt.toEpochMilli(),
    )

    private fun SavedAnswerEntity.toDomain() = SavedAnswer(
        id, gameProfileId, sourceMessageId, question, answer,
        runCatching {
            val array = JSONArray(citationsJson)
            (0 until array.length()).map { array.getJSONObject(it).toCitation() }
        }.getOrDefault(emptyList()),
        Instant.ofEpochMilli(createdAtEpochMillis),
    )

    private fun SourceCitation.toJson() = JSONObject().apply {
        put("title", title); put("sourceName", sourceName); put("url", url); put("excerpt", excerpt)
        put("retrievedAt", retrievedAt.toEpochMilli())
    }

    private fun JSONObject.toCitation() = SourceCitation(
        getString("title"), getString("sourceName"), getString("url"), getString("excerpt"),
        Instant.ofEpochMilli(getLong("retrievedAt")),
    )

    private fun GameNote.toEntity() = GameNoteEntity(id, gameProfileId, title, content, createdAt.toEpochMilli(), updatedAt.toEpochMilli())
    private fun GameNoteEntity.toDomain() = GameNote(
        id, gameProfileId, title, content, Instant.ofEpochMilli(createdAtEpochMillis), Instant.ofEpochMilli(updatedAtEpochMillis),
    )
    private fun GameChecklist.toEntity() = GameChecklistEntity(
        id, gameProfileId, title, createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
    )
    private fun ChecklistItem.toEntity(checklistId: String) = ChecklistItemEntity(id, checklistId, text, isChecked, position)
    private fun ChecklistWithItems.toDomain() = GameChecklist(
        checklist.id, checklist.gameProfileId, checklist.title,
        items.sortedBy { it.position }.map { ChecklistItem(it.id, it.text, it.isChecked, it.position) },
        Instant.ofEpochMilli(checklist.createdAtEpochMillis), Instant.ofEpochMilli(checklist.updatedAtEpochMillis),
    )
}
