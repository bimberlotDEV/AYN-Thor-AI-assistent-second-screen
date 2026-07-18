package com.gameside.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import com.gameside.domain.display.DeviceDisplayInfo
import com.gameside.domain.display.DisplayRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class AndroidDisplayRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DisplayRepository {
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    override fun observeDisplays(): Flow<List<DeviceDisplayInfo>> = callbackFlow {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = emitSnapshot()
            override fun onDisplayRemoved(displayId: Int) = emitSnapshot()
            override fun onDisplayChanged(displayId: Int) = emitSnapshot()
            private fun emitSnapshot() { trySend(snapshot()) }
        }
        displayManager.registerDisplayListener(listener, null)
        trySend(snapshot())
        awaitClose { displayManager.unregisterDisplayListener(listener) }
    }.conflate()

    private fun snapshot(): List<DeviceDisplayInfo> = displayManager.displays
        .map(::toDomain)
        .sortedWith(compareBy<DeviceDisplayInfo> { !it.isDefault }.thenBy { it.id })

    @Suppress("DEPRECATION")
    private fun toDomain(display: Display): DeviceDisplayInfo {
        val size = Point().also(display::getRealSize)
        val displayContext = context.createDisplayContext(display)
        val configuration = displayContext.resources.configuration
        val flags = display.flags
        val hasSecondaryFeature = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
        )
        val isPrivate = flags and Display.FLAG_PRIVATE != 0
        val appLaunchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName)
        val canHost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(ActivityManager::class.java)
                .isActivityStartAllowedOnDisplay(context, display.displayId, appLaunchIntent)
        } else {
            display.displayId == Display.DEFAULT_DISPLAY || (hasSecondaryFeature && !isPrivate)
        }

        return DeviceDisplayInfo(
            id = display.displayId,
            name = display.name,
            widthPixels = size.x,
            heightPixels = size.y,
            densityDpi = configuration.densityDpi,
            refreshRateHz = display.refreshRate,
            rotationDegrees = when (display.rotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            },
            isDefault = display.displayId == Display.DEFAULT_DISPLAY,
            isPresentation = flags and Display.FLAG_PRESENTATION != 0,
            isPrivate = isPrivate,
            isSecure = flags and Display.FLAG_SECURE != 0,
            hasTouch = configuration.touchscreen != Configuration.TOUCHSCREEN_NOTOUCH,
            canHostActivities = canHost,
        )
    }
}
