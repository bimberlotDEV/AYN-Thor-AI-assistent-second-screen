package com.gameside.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameside.core.design.GameSideTheme
import com.gameside.device.CompanionLaunchResult
import com.gameside.device.SecondaryDisplayLauncher
import com.gameside.features.display.DisplayDashboardRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var displayLauncher: SecondaryDisplayLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSideTheme {
                DisplayDashboardRoute(
                    onLaunchCompanion = { displayId -> launchCompanion(displayId) },
                    onOpenSingleScreen = { launchCompanion(null) },
                )
            }
        }
    }

    private fun launchCompanion(displayId: Int?): CompanionLaunchResult {
        val intent = Intent(this, CompanionActivity::class.java)
        return displayLauncher.launch(this, intent, displayId)
    }
}
