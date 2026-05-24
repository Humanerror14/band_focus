package com.bandfocus.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bandfocus.app.domain.model.DownloadMode

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val twoColumns = maxWidth >= 840.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DefaultModeSetting(
                            selectedMode = uiState.defaultMode,
                            threadCount = uiState.defaultThreadCount,
                            onSelectMode = viewModel::setDefaultMode
                        )
                        SettingRow("Theme", uiState.theme.replaceFirstChar { it.uppercase() }, Icons.Default.DarkMode)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SettingRow(
                            title = "Wi-Fi only",
                            subtitle = "Download on trusted networks",
                            icon = Icons.Default.Wifi,
                            checked = uiState.wifiOnly,
                            onCheckedChange = viewModel::setWifiOnly
                        )
                        SettingRow(
                            title = "Auto Focus Mode",
                            subtitle = "Enable during large downloads",
                            icon = Icons.Default.Security,
                            checked = uiState.autoFocusMode,
                            onCheckedChange = viewModel::setAutoFocusMode
                        )
                    }
                }
            } else {
                DefaultModeSetting(
                    selectedMode = uiState.defaultMode,
                    threadCount = uiState.defaultThreadCount,
                    onSelectMode = viewModel::setDefaultMode
                )
                SettingRow("Theme", uiState.theme.replaceFirstChar { it.uppercase() }, Icons.Default.DarkMode)
                SettingRow(
                    title = "Wi-Fi only",
                    subtitle = "Download on trusted networks",
                    icon = Icons.Default.Wifi,
                    checked = uiState.wifiOnly,
                    onCheckedChange = viewModel::setWifiOnly
                )
                SettingRow(
                    title = "Auto Focus Mode",
                    subtitle = "Enable during large downloads",
                    icon = Icons.Default.Security,
                    checked = uiState.autoFocusMode,
                    onCheckedChange = viewModel::setAutoFocusMode
                )
            }
            SettingRow("Notifications", "Progress, pause, cancel, and complete", Icons.Default.Notifications)
        }
    }
}

@Composable
private fun DefaultModeSetting(
    selectedMode: DownloadMode,
    threadCount: Int,
    onSelectMode: (DownloadMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Default mode", fontWeight = FontWeight.SemiBold)
                    Text("${selectedMode.label()} • $threadCount threads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (checked != null) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
            }
        }
    }
}

private fun DownloadMode.label(): String = when (this) {
    DownloadMode.ECO -> "Eco"
    DownloadMode.BALANCED -> "Balanced"
    DownloadMode.TURBO -> "Turbo"
    DownloadMode.NIGHT -> "Night"
}
