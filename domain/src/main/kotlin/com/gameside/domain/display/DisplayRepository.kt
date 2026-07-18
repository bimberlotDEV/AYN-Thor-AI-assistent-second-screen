package com.gameside.domain.display

import kotlinx.coroutines.flow.Flow

interface DisplayRepository {
    fun observeDisplays(): Flow<List<DeviceDisplayInfo>>
}
