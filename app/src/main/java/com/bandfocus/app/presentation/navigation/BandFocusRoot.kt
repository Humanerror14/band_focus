package com.bandfocus.app.presentation.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandfocus.app.R
import com.bandfocus.app.core.design.BandFocusSlate950
import com.bandfocus.app.presentation.downloads.DownloadsScreen
import com.bandfocus.app.presentation.focus.FocusModeScreen
import com.bandfocus.app.presentation.home.HomeScreen
import com.bandfocus.app.presentation.insights.InsightsScreen
import com.bandfocus.app.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "Home", Icons.Outlined.Home),
    Downloads("downloads", "Download", Icons.Outlined.Download),
    Focus("focus", "Focus", Icons.Outlined.Security),
    Insights("insights", "Insights", Icons.Outlined.Analytics),
    Settings("settings", "More", Icons.Default.MoreHoriz)
}

@Composable
fun BandFocusRoot() {
    var selectedDestination by rememberSaveable { mutableStateOf(TopLevelDestination.Home) }
    val stateHolder = rememberSaveableStateHolder()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        if (useRail) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavigationRail(
                    containerColor = BandFocusSlate950,
                    contentColor = Color.White,
                    header = {
                        Column(
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.bandfocus_logo),
                                contentDescription = "BandFocus logo",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Text("BandFocus", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = selectedDestination == destination
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (selectedDestination != destination) selectedDestination = destination
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF123C8C),
                                unselectedIconColor = Color(0xFFCBD5E1),
                                unselectedTextColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
                BandFocusDestinationContent(
                    destination = selectedDestination,
                    stateHolder = stateHolder,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .height(72.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = selectedDestination == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (selectedDestination != destination) selectedDestination = destination
                                },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(21.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        destination.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                BandFocusDestinationContent(
                    destination = selectedDestination,
                    stateHolder = stateHolder,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun BandFocusDestinationContent(
    destination: TopLevelDestination,
    stateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(destination) {
        alpha.snapTo(0.72f)
        offsetY.snapTo(24f)
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160)
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        stateHolder.SaveableStateProvider(destination.route) {
            when (destination) {
                TopLevelDestination.Home -> HomeScreen()
                TopLevelDestination.Downloads -> DownloadsScreen()
                TopLevelDestination.Focus -> FocusModeScreen()
                TopLevelDestination.Insights -> InsightsScreen()
                TopLevelDestination.Settings -> SettingsScreen()
            }
        }
    }
}
