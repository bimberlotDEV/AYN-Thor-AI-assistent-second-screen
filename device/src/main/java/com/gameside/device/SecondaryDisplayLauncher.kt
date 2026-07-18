package com.gameside.device

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.view.Display
import javax.inject.Inject

sealed interface CompanionLaunchResult {
    data class Success(val displayId: Int) : CompanionLaunchResult
    data class Failure(val reason: String) : CompanionLaunchResult
}

class SecondaryDisplayLauncher @Inject constructor() {
    fun launch(activity: Activity, intent: Intent, displayId: Int?): CompanionLaunchResult {
        val target = displayId ?: currentDisplayId(activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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


    @Suppress("DEPRECATION")
    private fun currentDisplayId(activity: Activity): Int = if (Build.VERSION.SDK_INT >= 30) {
        activity.display?.displayId ?: Display.DEFAULT_DISPLAY
    } else {
        activity.windowManager.defaultDisplay.displayId
    }
}
