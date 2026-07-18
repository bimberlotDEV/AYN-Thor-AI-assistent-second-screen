package com.gameside.features.wiki

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.game.GameProfile
import com.gameside.domain.game.GameProfileRepository
import com.gameside.domain.knowledge.GameKnowledgeProvider
import com.gameside.domain.knowledge.KnowledgeCacheRepository
import com.gameside.domain.knowledge.KnowledgeDocument
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WikiState(
    val game: GameProfile? = null,
    val documents: List<KnowledgeDocument> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null,
)

private data class WikiInputs(val query: String, val loading: Boolean, val online: Boolean, val error: String?)
private data class WikiContent(val game: GameProfile?, val documents: List<KnowledgeDocument>)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class WikiViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    settings: SettingsRepository,
    games: GameProfileRepository,
    private val provider: GameKnowledgeProvider,
    private val cache: KnowledgeCacheRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val online = MutableStateFlow(isOnline())
    private val error = MutableStateFlow<String?>(null)

    private val content = settings.settings.flatMapLatest { appSettings ->
        val gameId = appSettings.activeGameId ?: return@flatMapLatest flowOf(WikiContent(null, emptyList()))
        combine(games.observeById(gameId), cache.observeCached(gameId)) { game, docs -> WikiContent(game, docs) }
    }
    private val inputs = combine(query, loading, online, error, ::WikiInputs)

    val state: StateFlow<WikiState> = combine(content, inputs) { content, inputs ->
        WikiState(content.game, content.documents, inputs.query, inputs.loading, inputs.online, inputs.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WikiState())

    fun setQuery(value: String) { query.value = value.take(250) }
    fun dismissError() { error.value = null }

    fun search() {
        val game = state.value.game ?: return
        val value = state.value.query.trim()
        if (value.isEmpty() || loading.value) return
        online.value = isOnline()
        if (!online.value) { error.value = "Offline: showing downloaded wiki pages."; return }
        viewModelScope.launch {
            loading.value = true
            error.value = null
            try {
                val results = provider.search(game, value).take(8)
                results.map { result -> async { runCatching { provider.retrieve(result) }.getOrNull() } }.awaitAll()
                if (results.isEmpty()) error.value = "No matching page was found on this game's wiki."
            } catch (throwable: Throwable) {
                online.value = isOnline()
                error.value = throwable.message ?: "The game wiki could not be searched."
            } finally {
                loading.value = false
            }
        }
    }

    fun clearCache() {
        val gameId = state.value.game?.id ?: return
        viewModelScope.launch { cache.clear(gameId) }
    }

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
