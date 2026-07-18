package com.gameside.device

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.view.Display
import javax.inject.Inject

sealed interface GameLaunchResult {
    data object Success : GameLaunchResult
    data class Failure(val reason: String) : GameLaunchResult
}

class GameLauncher @Inject constructor() {
    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

    fun launchOnPrimary(activity: Activity, packageName: String): GameLaunchResult {
        val normalized = packageName.trim()
        if (!packagePattern.matches(normalized)) {
            return GameLaunchResult.Failure("Enter a valid Android package name")
        }
        val intent = activity.packageManager.getLaunchIntentForPackage(normalized)
            ?: return GameLaunchResult.Failure("No launchable app found for $normalized")

        return try {
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = Display.DEFAULT_DISPLAY }
            activity.startActivity(intent, options.toBundle())
            GameLaunchResult.Success
        } catch (error: SecurityException) {
            GameLaunchResult.Failure("Android blocked this app from launching on the primary display")
        } catch (error: ActivityNotFoundException) {
            GameLaunchResult.Failure("The selected game is no longer installed")
        }
    }
}
