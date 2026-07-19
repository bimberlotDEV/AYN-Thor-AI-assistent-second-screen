package com.gameside.features.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameside.domain.display.DeviceDisplayInfo
import com.gameside.domain.display.DisplayRepository
import com.gameside.domain.display.DisplaySelectionPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.gameside.domain.settings.SettingsRepository
import com.gameside.device.CompanionRestoreTrigger
import com.gameside.device.CompanionSessionCoordinator
import com.gameside.device.CompanionSessionStatus

data class DisplayDashboardState(
    val displays: List<DeviceDisplayInfo> = emptyList(),
    val preferredDisplayId: Int? = null,
    val shortcutEnabled: Boolean = false,
    val shortcutKeyCode: Int = 108,
    val longPressMillis: Int = 800,
    val calibrationPending: Boolean = false,
    val companionStatus: CompanionSessionStatus = CompanionSessionStatus.inactive,
    val companionSessionActive: Boolean = false,
    val keepCompanionActive: Boolean = true,
    val targetDisplayId: Int? = null,
    val restoreAttempts: Int = 0,
    val companionError: String? = null,
)

@HiltViewModel
class DisplayDashboardViewModel @Inject constructor(
    repository: DisplayRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionCoordinator: CompanionSessionCoordinator,
) : ViewModel() {
    val state: StateFlow<DisplayDashboardState> = combine(repository.observeDisplays(), settingsRepository.settings, sessionCoordinator.state) { displays, settings, session ->
            DisplayDashboardState(
                displays = displays,
                preferredDisplayId = DisplaySelectionPolicy.preferred(displays)?.id,
                shortcutEnabled = settings.controllerShortcutEnabled,
                shortcutKeyCode = settings.controllerShortcutKeyCode,
                longPressMillis = settings.controllerShortcutLongPressMillis,
                calibrationPending = settings.controllerCalibrationPending,
                companionStatus = session.status,
                companionSessionActive = session.sessionActive,
                keepCompanionActive = session.keepActive,
                targetDisplayId = session.targetDisplayId,
                restoreAttempts = session.attemptsLastMinute,
                companionError = session.lastError,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayDashboardState())

    fun setShortcutEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setControllerShortcutEnabled(enabled) } }
    fun startCalibration() { viewModelScope.launch { settingsRepository.setControllerCalibrationPending(true) } }
    fun setLongPressMillis(value: Int) { viewModelScope.launch { settingsRepository.setControllerShortcutLongPressMillis(value) } }
    fun setKeepCompanionActive(enabled: Boolean) = sessionCoordinator.setKeepActive(enabled)
    fun restoreCompanionNow() {
        if (!sessionCoordinator.state.value.sessionActive) {
            state.value.displays.firstOrNull { !it.isDefault && it.canHostActivities }?.let { sessionCoordinator.startSession(it.id) }
        }
        sessionCoordinator.requestRestore(CompanionRestoreTrigger.Manual)
    }
    fun stopCompanionSession() = sessionCoordinator.stopSession()
    fun diagnosticsText(): String = sessionCoordinator.diagnosticsText()
}
