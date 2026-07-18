package com.gameside.features.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.personal.ChecklistItem
import com.gameside.domain.personal.GameChecklist
import com.gameside.domain.personal.GameNote
import com.gameside.domain.personal.PersonalTools
import com.gameside.domain.personal.PersonalToolsRepository
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PersonalToolsState(val gameProfileId: String? = null, val tools: PersonalTools = PersonalTools())

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonalToolsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val repository: PersonalToolsRepository,
) : ViewModel() {
    val state: StateFlow<PersonalToolsState> = settings.settings.flatMapLatest { appSettings ->
        val gameId = appSettings.activeGameId ?: return@flatMapLatest flowOf(PersonalToolsState())
        repository.observe(gameId).map { PersonalToolsState(gameId, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalToolsState())

    fun addNote(title: String, content: String) {
        val gameId = state.value.gameProfileId ?: return
        val now = Instant.now()
        viewModelScope.launch {
            repository.saveNote(GameNote(UUID.randomUUID().toString(), gameId, title.trim(), content.trim(), now, now))
        }
    }

    fun addChecklist(title: String, lines: List<String>) {
        val gameId = state.value.gameProfileId ?: return
        val now = Instant.now()
        val id = UUID.randomUUID().toString()
        val items = lines.mapIndexed { index, text -> ChecklistItem(UUID.randomUUID().toString(), text.trim(), false, index) }
        viewModelScope.launch { repository.saveChecklist(GameChecklist(id, gameId, title.trim(), items, now, now)) }
    }

    fun toggleItem(item: ChecklistItem) = viewModelScope.launch { repository.setChecklistItemChecked(item.id, !item.isChecked) }
    fun deleteSaved(id: String) = viewModelScope.launch { repository.deleteSavedAnswer(id) }
    fun deleteNote(id: String) = viewModelScope.launch { repository.deleteNote(id) }
    fun deleteChecklist(id: String) = viewModelScope.launch { repository.deleteChecklist(id) }
}
