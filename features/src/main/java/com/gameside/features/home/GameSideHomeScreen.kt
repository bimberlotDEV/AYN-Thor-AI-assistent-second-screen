package com.gameside.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gameside.device.CompanionLaunchResult
import com.gameside.features.display.DisplayDashboardRoute
import com.gameside.features.game.GameLibraryRoute
import com.gameside.features.onboarding.OnboardingScreen

@Composable
fun GameSideHomeRoute(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        HomeState.Loading -> Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }
        HomeState.Onboarding -> OnboardingScreen(onComplete = viewModel::completeOnboarding)
        is HomeState.Content -> HomeContent(onLaunchCompanion, onOpenSingleScreen)
    }
}

@Composable
private fun HomeContent(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = null) },
                    label = { Text("Games") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.DeveloperBoard, contentDescription = null) },
                    label = { Text("Displays") },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> GameLibraryRoute(Modifier.padding(padding))
            else -> DisplayDashboardRoute(
                onLaunchCompanion = onLaunchCompanion,
                onOpenSingleScreen = onOpenSingleScreen,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
