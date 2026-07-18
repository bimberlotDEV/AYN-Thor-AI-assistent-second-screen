package com.gameside.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameside.core.design.GameSideTheme
import com.gameside.device.GameLauncher
import com.gameside.features.companion.CompanionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CompanionActivity : ComponentActivity() {
    @Inject lateinit var gameLauncher: GameLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSideTheme {
                CompanionScreen(
                    displayId = currentDisplayId(),
                    onLaunchGame = { packageName -> gameLauncher.launchOnPrimary(this, packageName) },
                    onClose = { finishAndRemoveTask() },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayId(): Int = if (android.os.Build.VERSION.SDK_INT >= 30) {
        display?.displayId ?: android.view.Display.DEFAULT_DISPLAY
    } else {
        windowManager.defaultDisplay.displayId
    }
}
