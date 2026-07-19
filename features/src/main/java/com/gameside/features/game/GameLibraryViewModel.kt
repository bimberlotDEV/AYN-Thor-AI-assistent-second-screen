package com.gameside.features.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.game.GamePlatform
import com.gameside.domain.game.DiscoveredGame
import com.gameside.domain.game.GameDiscoveryRepository
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
    val discoveredGames: List<DiscoveredGame> = emptyList(),
    val isDetecting: Boolean = false,
    val discoveryMessage: String? = null,
)

data class GameProfileDraft(
    val id: String? = null,
    val title: String = "",
    val packageName: String = "",
    val preferredWikiSource: String = "",
    val platform: GamePlatform = GamePlatform.ANDROID,
    val spoilerLevel: SpoilerLevel = SpoilerLevel.MINIMAL,
    val isPinned: Boolean = false,
    val createdAt: Instant? = null,
)

@HiltViewModel
class GameLibraryViewModel @Inject constructor(
    private val repository: GameProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val discoveryRepository: GameDiscoveryRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val discoveredGames = MutableStateFlow<List<DiscoveredGame>>(emptyList())
    private val isDetecting = MutableStateFlow(false)
    private val discoveryMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<GameLibraryState> = combine(
        repository.observeAll(),
        settingsRepository.settings,
        searchQuery,
        combine(discoveredGames, isDetecting, discoveryMessage) { discovered, detecting, message -> Triple(discovered, detecting, message) },
    ) { games, settings, query, discovery ->
        val packageNames = games.flatMap { it.packageNames }.toSet()
        val titles = games.map { it.title.trim().lowercase() }.toSet()
        GameLibraryState(
            games = games.filter { it.title.contains(query, ignoreCase = true) },
            activeGameId = settings.activeGameId,
            searchQuery = query,
            discoveredGames = discovery.first.filterNot {
                (it.packageName != null && it.packageName in packageNames) || it.title.trim().lowercase() in titles
            },
            isDetecting = discovery.second,
            discoveryMessage = discovery.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameLibraryState())

    init { refreshDetectedGames() }

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
                    preferredWikiSources = listOfNotNull(draft.preferredWikiSource.trim().takeIf(String::isNotEmpty)),
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

    fun refreshDetectedGames() {
        viewModelScope.launch {
            isDetecting.value = true
            discoveryMessage.value = null
            val detected = runCatching { discoveryRepository.detectInstalledGamesAndEmulators() }
                .getOrElse {
                    discoveryMessage.value = "Game detection failed: ${it.javaClass.simpleName}"
                    emptyList()
                }
            discoveredGames.value = detected
            if (detected.isEmpty() && discoveryMessage.value == null) {
                discoveryMessage.value = "No new Android games or emulator apps were detected."
            }
            isDetecting.value = false
        }
    }

    fun scanRomFolder(treeUri: String) {
        viewModelScope.launch {
            isDetecting.value = true
            discoveryMessage.value = "Scanning the selected emulator folder…"
            val roms = runCatching { discoveryRepository.scanRomTree(treeUri) }
                .getOrElse {
                    discoveryMessage.value = "ROM scan failed: ${it.javaClass.simpleName}"
                    emptyList()
                }
            discoveredGames.value = (discoveredGames.value + roms).distinctBy { it.stableKey }
            discoveryMessage.value = if (roms.isEmpty()) {
                "No supported ROM files found in that folder."
            } else {
                "Detected ${roms.size} ROM${if (roms.size == 1) "" else "s"}. Choose Import all or import them separately."
            }
            isDetecting.value = false
        }
    }

    fun importDetected(game: DiscoveredGame) {
        viewModelScope.launch { importProfile(game, setActiveWhenEmpty = true) }
    }

    fun importAllDetected() {
        val suggestions = state.value.discoveredGames
        if (suggestions.isEmpty()) return
        viewModelScope.launch {
            val shouldSetActive = state.value.activeGameId == null
            var firstId: String? = null
            suggestions.forEach { discovered ->
                val id = importProfile(discovered, setActiveWhenEmpty = false)
                if (firstId == null) firstId = id
            }
            if (shouldSetActive) settingsRepository.setActiveGame(firstId)
            discoveryMessage.value = "Imported ${suggestions.size} detected game${if (suggestions.size == 1) "" else "s"}."
        }
    }

    private suspend fun importProfile(game: DiscoveredGame, setActiveWhenEmpty: Boolean): String {
        val now = Instant.now()
        val id = UUID.nameUUIDFromBytes("gameside:${game.stableKey}".toByteArray(Charsets.UTF_8)).toString()
        repository.upsert(
            GameProfile(
                id = id,
                title = game.title,
                packageNames = listOfNotNull(game.packageName),
                platform = game.platform,
                coverImageUri = null,
                preferredWikiSources = emptyList(),
                spoilerLevel = SpoilerLevel.MINIMAL,
                playerProgress = null,
                customSystemPrompt = null,
                isPinned = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (setActiveWhenEmpty && state.value.activeGameId == null) settingsRepository.setActiveGame(id)
        return id
    }
}
