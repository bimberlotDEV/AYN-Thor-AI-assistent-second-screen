package com.gameside.device

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import javax.inject.Inject

sealed interface CompanionLaunchResult {
    data class Success(val displayId: Int) : CompanionLaunchResult
    data class Failure(val reason: String) : CompanionLaunchResult
}

internal object LowerScreenLaunchPolicy {
    fun targetDisplay(currentDisplayId: Int, availableDisplayIds: Collection<Int>): Int? {
        if (currentDisplayId != Display.DEFAULT_DISPLAY) return null
        return availableDisplayIds.firstOrNull { it != Display.DEFAULT_DISPLAY }
    }
}

class SecondaryDisplayLauncher @Inject constructor() {
    fun isOnSecondaryDisplay(activity: Activity): Boolean = currentDisplayId(activity) != Display.DEFAULT_DISPLAY

    fun lowerDisplayFor(activity: Activity): Int? {
        return LowerScreenLaunchPolicy.targetDisplay(
            currentDisplayId(activity),
            activity.getSystemService(DisplayManager::class.java).displays.map { it.displayId },
        )
    }

    fun launch(activity: Activity, intent: Intent, displayId: Int?): CompanionLaunchResult {
        val target = displayId ?: currentDisplayId(activity)
        addCompanionFlags(intent)
        return try {
            if (displayId == null) {
                activity.startActivity(intent)
            } else {
                val options = ActivityOptions.makeBasic().apply { launchDisplayId = displayId }
                activity.startActivity(intent, options.toBundle())
            }
            CompanionLaunchResult.Success(target)
        } catch (error: SecurityException) {
            CompanionLaunchResult.Failure("Android blocked activity launch on display $target")
        } catch (error: ActivityNotFoundException) {
            CompanionLaunchResult.Failure("Companion activity is unavailable")
        } catch (error: IllegalArgumentException) {
            CompanionLaunchResult.Failure("Display $target is no longer available")
        }
    }

    private fun addCompanionFlags(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }


    @Suppress("DEPRECATION")
    private fun currentDisplayId(activity: Activity): Int = if (Build.VERSION.SDK_INT >= 30) {
        activity.display?.displayId ?: Display.DEFAULT_DISPLAY
    } else {
        activity.windowManager.defaultDisplay.displayId
    }
}
