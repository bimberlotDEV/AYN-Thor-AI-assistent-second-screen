package com.gameside.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.ai.AiAnswerRequest
import com.gameside.domain.ai.AiChatMessage
import com.gameside.domain.ai.AiCredentialIds
import com.gameside.domain.ai.TextAiProvider
import com.gameside.domain.security.CredentialStore
import com.gameside.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProviderSettingsState(
    val keyDraft: String = "",
    val hasStoredKey: Boolean = false,
    val model: String = "deepseek-v4-flash",
    val isWorking: Boolean = false,
    val statusMessage: String? = null,
)

private data class ProviderInputs(val keyDraft: String, val hasKey: Boolean, val working: Boolean, val message: String?)

@HiltViewModel
class ProviderSettingsViewModel @Inject constructor(
    private val credentials: CredentialStore,
    private val settingsRepository: SettingsRepository,
    private val provider: TextAiProvider,
) : ViewModel() {
    private val keyDraft = MutableStateFlow("")
    private val hasKey = MutableStateFlow(false)
    private val working = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val inputs = combine(keyDraft, hasKey, working, message) { keyDraft, hasKey, working, message ->
        ProviderInputs(keyDraft, hasKey, working, message)
    }

    val state: StateFlow<ProviderSettingsState> = combine(settingsRepository.settings, inputs) { settings, inputs ->
        ProviderSettingsState(
            keyDraft = inputs.keyDraft,
            hasStoredKey = inputs.hasKey,
            model = settings.aiModel,
            isWorking = inputs.working,
            statusMessage = inputs.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProviderSettingsState())

    init {
        viewModelScope.launch { hasKey.value = credentials.contains(AiCredentialIds.DEEPSEEK_API_KEY) }
    }

    fun setKeyDraft(value: String) {
        keyDraft.value = value.trim().take(MAX_KEY_CHARS)
        message.value = null
    }

    fun saveKey() {
        val key = keyDraft.value
        if (key.isBlank()) return
        viewModelScope.launch {
            working.value = true
            runCatching { credentials.put(AiCredentialIds.DEEPSEEK_API_KEY, key) }
                .onSuccess {
                    keyDraft.value = ""
                    hasKey.value = true
                    message.value = "API key encrypted and stored on this device."
                }
                .onFailure { message.value = it.message ?: "API key could not be stored." }
            working.value = false
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            credentials.remove(AiCredentialIds.DEEPSEEK_API_KEY)
            hasKey.value = false
            keyDraft.value = ""
            message.value = "Stored API key removed."
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch { settingsRepository.setAiModel(model) }
    }

    fun testConnection() {
        if (!state.value.hasStoredKey || state.value.isWorking) return
        viewModelScope.launch {
            working.value = true
            message.value = null
            runCatching {
                provider.generateAnswer(
                    AiAnswerRequest(
                        systemPrompt = "Reply with exactly: OK",
                        messages = listOf(AiChatMessage("user", "Connection test")),
                        model = state.value.model,
                        maxTokens = 16,
                    ),
                ).collect { }
            }.onSuccess { message.value = "DeepSeek connection successful." }
                .onFailure { message.value = it.message ?: "DeepSeek connection failed." }
            working.value = false
        }
    }

    private companion object { const val MAX_KEY_CHARS = 256 }
}
