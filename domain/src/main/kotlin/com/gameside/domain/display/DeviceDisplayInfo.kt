package com.gameside.domain.display

data class DeviceDisplayInfo(
    val id: Int,
    val name: String,
    val widthPixels: Int,
    val heightPixels: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val rotationDegrees: Int,
    val isDefault: Boolean,
    val isPresentation: Boolean,
    val isPrivate: Boolean,
    val isSecure: Boolean,
    val hasTouch: Boolean,
    val canHostActivities: Boolean,
) {
    val pixelArea: Long = widthPixels.toLong() * heightPixels.toLong()
    val signature: String = "$name:$widthPixels:$heightPixels:$densityDpi:$isPresentation"
}
