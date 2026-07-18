package com.gameside.features.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.privacy.LocalDataSummary
import com.gameside.domain.privacy.PrivacyRepository
import com.gameside.domain.backup.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val summary: LocalDataSummary = LocalDataSummary(),
    val isWorking: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val repository: PrivacyRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val working = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<PrivacyUiState> = combine(repository.summary, working, message) { summary, busy, text ->
        PrivacyUiState(summary, busy, text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyUiState())

    fun clearConversations() = run("Conversation history removed") { repository.clearConversations() }
    fun clearPersonalTools() = run("Saved answers, notes, and checklists removed") { repository.clearPersonalTools() }
    fun clearWikiCache() = run("Downloaded wiki pages removed") { repository.clearWikiCache() }
    fun removeCredential() = run("DeepSeek API key removed") { repository.removeProviderCredential() }
    fun resetAllData() = run("All local data removed") { repository.resetAllData() }
    fun exportTo(uri: String) = run("Backup exported. API credentials and wiki cache were not included.") { backupRepository.exportTo(uri) }
    fun importFrom(uri: String) = runImport(uri)
    fun dismissMessage() { message.value = null }

    private fun run(success: String, action: suspend () -> Unit) {
        if (working.value) return
        viewModelScope.launch {
            working.value = true
            message.value = null
            runCatching { action() }
                .onSuccess { message.value = success }
                .onFailure { message.value = "Could not remove data. Try again." }
            working.value = false
        }
    }

    private fun runImport(uri: String) {
        if (working.value) return
        viewModelScope.launch {
            working.value = true
            message.value = null
            runCatching { backupRepository.importFrom(uri) }
                .onSuccess { result -> message.value = "Imported ${result.games} games, ${result.conversations} conversations, and ${result.personalItems} personal items." }
                .onFailure { message.value = "This backup could not be imported. No unvalidated data was added." }
            working.value = false
        }
    }
}
