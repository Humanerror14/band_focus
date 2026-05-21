package com.bandfocus.app.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bandfocus.app.presentation.downloads.DownloadsScreen
import com.bandfocus.app.presentation.focus.FocusModeScreen
import com.bandfocus.app.presentation.home.HomeScreen
import com.bandfocus.app.presentation.insights.InsightsScreen
import com.bandfocus.app.presentation.settings.SettingsScreen

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Default.Home),
    Downloads("downloads", "Downloads", Icons.Default.Download),
    Focus("focus", "Focus", Icons.Default.Security),
    Insights("insights", "Insights", Icons.Default.Analytics),
    Settings("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun BandFocusRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopLevelDestination.Home.route) { HomeScreen() }
            composable(TopLevelDestination.Downloads.route) { DownloadsScreen() }
            composable(TopLevelDestination.Focus.route) { FocusModeScreen() }
            composable(TopLevelDestination.Insights.route) { InsightsScreen() }
            composable(TopLevelDestination.Settings.route) { SettingsScreen() }
        }
    }
}
