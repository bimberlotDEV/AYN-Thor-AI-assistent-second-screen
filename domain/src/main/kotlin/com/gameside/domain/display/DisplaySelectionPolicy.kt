package com.gameside.domain.display

object DisplaySelectionPolicy {
    fun preferred(displays: List<DeviceDisplayInfo>, savedSignature: String? = null): DeviceDisplayInfo? {
        val eligible = displays.filter { !it.isDefault && it.canHostActivities }
        return eligible.firstOrNull { it.signature == savedSignature }
            ?: eligible.minWithOrNull(compareBy<DeviceDisplayInfo> { it.pixelArea }.thenBy { it.id })
    }
}
