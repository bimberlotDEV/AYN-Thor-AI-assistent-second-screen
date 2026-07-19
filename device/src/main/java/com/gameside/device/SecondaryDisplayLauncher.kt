package com.gameside.device

import android.app.Activity
import android.app.ActivityOptions
import android.app.ActivityManager
import android.content.Context
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

class SecondaryDisplayLauncher @Inject constructor() {
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

    fun restore(context: Context, intent: Intent, displayId: Int): CompanionRestoreResult {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(displayId)
            ?: return CompanionRestoreResult.Failed("Display $displayId is no longer available")
        if (displayId == Display.DEFAULT_DISPLAY) return CompanionRestoreResult.Failed("Automatic restore requires a secondary display")
        addCompanionFlags(intent)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val allowed = context.getSystemService(ActivityManager::class.java)
                    .isActivityStartAllowedOnDisplay(context, display.displayId, intent)
                if (!allowed) return CompanionRestoreResult.Failed("Android blocked activities on display $displayId")
            }
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = displayId }
            context.startActivity(intent, options.toBundle())
            CompanionRestoreResult.Requested(displayId)
        } catch (error: SecurityException) {
            CompanionRestoreResult.Failed("SecurityException while restoring on display $displayId")
        } catch (error: ActivityNotFoundException) {
            CompanionRestoreResult.Failed("Companion activity is unavailable")
        } catch (error: IllegalArgumentException) {
            CompanionRestoreResult.Failed("Display $displayId rejected the companion activity")
        } catch (error: IllegalStateException) {
            CompanionRestoreResult.Failed("Android could not restore the companion task")
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
