package com.gameside.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
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
import com.gameside.device.GameLaunchResult
import com.gameside.features.chat.ChatRoute
import com.gameside.features.game.GameLibraryRoute
import com.gameside.features.onboarding.OnboardingScreen
import com.gameside.features.personal.PersonalToolsRoute
import com.gameside.features.wiki.WikiRoute

@Composable
fun GameSideHomeRoute(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
    onLaunchGame: (String) -> GameLaunchResult,
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
        is HomeState.Content -> HomeContent(onLaunchCompanion, onOpenSingleScreen, onLaunchGame)
    }
}

@Composable
private fun HomeContent(
    onLaunchCompanion: (Int) -> CompanionLaunchResult,
    onOpenSingleScreen: () -> CompanionLaunchResult,
    onLaunchGame: (String) -> GameLaunchResult,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null) },
                    label = { Text("Ask") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                    label = { Text("Wiki") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Bookmarks, contentDescription = null) },
                    label = { Text("Saved") },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.SportsEsports, contentDescription = null) },
                    label = { Text("Games") },
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = null) },
                    label = { Text("More") },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> ChatRoute(Modifier.padding(padding))
            1 -> WikiRoute(Modifier.padding(padding))
            2 -> PersonalToolsRoute(Modifier.padding(padding))
            3 -> GameLibraryRoute(onLaunchGame, Modifier.padding(padding))
            else -> MoreRoute(onLaunchCompanion, onOpenSingleScreen, Modifier.padding(padding))
        }
    }
}
