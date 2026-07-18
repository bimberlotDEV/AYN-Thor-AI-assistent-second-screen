package com.gameside.features.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.game.SpoilerLevel
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameLibraryState(
    val games: List<GameProfile> = emptyList(),
    val activeGameId: String? = null,
    val searchQuery: String = "",
)

data class GameProfileDraft(
    val id: String? = null,
    val title: String = "",
    val packageName: String = "",
    val platform: GamePlatform = GamePlatform.ANDROID,
    val spoilerLevel: SpoilerLevel = SpoilerLevel.MINIMAL,
    val isPinned: Boolean = false,
    val createdAt: Instant? = null,
)

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    private val repository: GameProfileRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<GameLibraryState> = combine(
        repository.observeAll(),
        settingsRepository.settings,
        searchQuery,
    ) { games, settings, query ->
        GameLibraryState(
            games = games.filter { it.title.contains(query, ignoreCase = true) },
            activeGameId = settings.activeGameId,
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameLibraryState())

    fun setSearchQuery(value: String) { searchQuery.value = value }

    fun save(draft: GameProfileDraft) {
        val title = draft.title.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            val now = Instant.now()
            val id = draft.id ?: UUID.randomUUID().toString()
            repository.upsert(
                GameProfile(
                    id = id,
                    title = title,
                    packageNames = listOfNotNull(draft.packageName.trim().takeIf(String::isNotEmpty)),
                    platform = draft.platform,
                    coverImageUri = null,
                    preferredWikiSources = emptyList(),
                    spoilerLevel = draft.spoilerLevel,
                    playerProgress = null,
                    customSystemPrompt = null,
                    isPinned = draft.isPinned,
                    isArchived = false,
                    createdAt = draft.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            if (state.value.activeGameId == null) settingsRepository.setActiveGame(id)
        }
    }

    fun setActive(id: String) {
        viewModelScope.launch { settingsRepository.setActiveGame(id) }
    }

    fun delete(game: GameProfile) {
        viewModelScope.launch {
            repository.delete(game.id)
            if (state.value.activeGameId == game.id) settingsRepository.setActiveGame(null)
        }
    }

    fun togglePin(game: GameProfile) {
        viewModelScope.launch { repository.upsert(game.copy(isPinned = !game.isPinned, updatedAt = Instant.now())) }
    }
}
