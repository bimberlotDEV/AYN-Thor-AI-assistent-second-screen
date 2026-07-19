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

data class DisplayDashboardState(
    val displays: List<DeviceDisplayInfo> = emptyList(),
    val preferredDisplayId: Int? = null,
    val shortcutEnabled: Boolean = false,
    val shortcutKeyCode: Int = 108,
    val longPressMillis: Int = 800,
    val calibrationPending: Boolean = false,
)

@HiltViewModel
class DisplayDashboardViewModel @Inject constructor(
    repository: DisplayRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<DisplayDashboardState> = combine(repository.observeDisplays(), settingsRepository.settings) { displays, settings ->
            DisplayDashboardState(
                displays = displays,
                preferredDisplayId = DisplaySelectionPolicy.preferred(displays)?.id,
                shortcutEnabled = settings.controllerShortcutEnabled,
                shortcutKeyCode = settings.controllerShortcutKeyCode,
                longPressMillis = settings.controllerShortcutLongPressMillis,
                calibrationPending = settings.controllerCalibrationPending,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayDashboardState())

    fun setShortcutEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setControllerShortcutEnabled(enabled) } }
    fun startCalibration() { viewModelScope.launch { settingsRepository.setControllerCalibrationPending(true) } }
    fun setLongPressMillis(value: Int) { viewModelScope.launch { settingsRepository.setControllerShortcutLongPressMillis(value) } }
}
