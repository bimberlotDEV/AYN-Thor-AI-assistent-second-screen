package com.gameside.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameside.core.design.GameSideTheme
import com.gameside.device.CompanionLaunchResult
import com.gameside.device.SecondaryDisplayLauncher
import com.gameside.device.GameLauncher
import com.gameside.features.home.GameSideHomeRoute
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CompanionActivity : ComponentActivity() {
    @Inject lateinit var displayLauncher: SecondaryDisplayLauncher
    @Inject lateinit var gameLauncher: GameLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSideTheme {
                GameSideHomeRoute(
                    onLaunchCompanion = { displayId -> launchCompanion(displayId) },
                    onOpenSingleScreen = { launchCompanion(null) },
                    onLaunchGame = { packageName -> gameLauncher.launchOnPrimary(this, packageName) },
                )
            }
        }
    }

    private fun launchCompanion(displayId: Int?): CompanionLaunchResult {
        val intent = Intent(this, CompanionActivity::class.java)
        return displayLauncher.launch(this, intent, displayId)
    }
}
