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

data class DisplayDashboardState(
    val displays: List<DeviceDisplayInfo> = emptyList(),
    val preferredDisplayId: Int? = null,
)

@HiltViewModel
class DisplayDashboardViewModel @Inject constructor(
    repository: DisplayRepository,
) : ViewModel() {
    val state: StateFlow<DisplayDashboardState> = repository.observeDisplays()
        .map { displays ->
            DisplayDashboardState(
                displays = displays,
                preferredDisplayId = DisplaySelectionPolicy.preferred(displays)?.id,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplayDashboardState())
}
