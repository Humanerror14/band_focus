package com.bandfocus.app.presentation.focus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.graphics.drawable.toBitmap
import com.bandfocus.app.service.FocusVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FocusModeScreen(viewModel: FocusViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var focusEnabled by rememberSaveable { mutableStateOf(false) }
    var vpnGranted by rememberSaveable { mutableStateOf(VpnService.prepare(context) == null) }
    var selectedFilter by rememberSaveable { mutableStateOf(FocusFilter.All) }
    val blockedPackages = remember(uiState.apps) {
        uiState.apps.filter { it.isBlocked }.map { it.packageName }
    }
    val blockedApps = remember(uiState.apps) {
        uiState.apps.filter { it.isBlocked }
    }
    LaunchedEffect(focusEnabled, blockedPackages) {
        if (focusEnabled) context.startFocusVpn(blockedPackages)
    }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnGranted = result.resultCode == Activity.RESULT_OK || VpnService.prepare(context) == null
        if (vpnGranted) {
            focusEnabled = true
            context.startFocusVpn(blockedPackages)
        }
    }
    val filteredApps = remember(search, selectedFilter, uiState.apps) {
        uiState.apps.filter { item ->
            val matchesSearch = search.isBlank() ||
                item.appName.contains(search, ignoreCase = true) ||
                item.packageName.contains(search, ignoreCase = true) ||
                item.accessLabel().contains(search, ignoreCase = true)
            matchesSearch && selectedFilter.matches(item)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val twoColumns = maxWidth >= 840.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Focus Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    FocusSummary(
                        checked = focusEnabled,
                        blockedCount = blockedPackages.size,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val permissionIntent = VpnService.prepare(context)
                                if (permissionIntent != null) {
                                    vpnGranted = false
                                    vpnPermissionLauncher.launch(permissionIntent)
                                } else {
                                    vpnGranted = true
                                    focusEnabled = true
                                    context.startFocusVpn(blockedPackages)
                                }
                            } else {
                                focusEnabled = false
                                context.stopFocusVpn()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    VpnStatus(
                        granted = vpnGranted,
                        onGrantClick = {
                            val permissionIntent = VpnService.prepare(context)
                            if (permissionIntent != null) {
                                vpnPermissionLauncher.launch(permissionIntent)
                            } else {
                                vpnGranted = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FocusSummary(
                    checked = focusEnabled,
                    blockedCount = blockedPackages.size,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val permissionIntent = VpnService.prepare(context)
                            if (permissionIntent != null) {
                                vpnGranted = false
                                vpnPermissionLauncher.launch(permissionIntent)
                            } else {
                                vpnGranted = true
                                focusEnabled = true
                                context.startFocusVpn(blockedPackages)
                            }
                        } else {
                            focusEnabled = false
                            context.stopFocusVpn()
                        }
                    }
                )
                VpnStatus(
                    granted = vpnGranted,
                    onGrantClick = {
                        val permissionIntent = VpnService.prepare(context)
                        if (permissionIntent != null) {
                            vpnPermissionLauncher.launch(permissionIntent)
                        } else {
                            vpnGranted = true
                        }
                    }
                )
            }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search apps, package, or blocked") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            BlockedInternetCard(
                blockedApps = blockedApps,
                focusEnabled = focusEnabled
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.label} (${filter.count(uiState.apps)})", maxLines = 1) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.applyPreset(preset) },
                        label = { Text(preset.label, maxLines = 1) }
                    )
                }
            }
            if (filteredApps.isEmpty()) {
                EmptyFocusSearchState(selectedFilter)
            } else {
                filteredApps.forEach { item ->
                    AppRuleRow(
                        item = item,
                        checked = item.isBlocked,
                        onCheckedChange = { checked ->
                            viewModel.setBlocked(item, checked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedInternetCard(
    blockedApps: List<FocusAppItem>,
    focusEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (blockedApps.isEmpty()) {
                MaterialTheme.colorScheme.surface
            } else {
                Color(0xFFFFF7ED)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Blocked internet access", fontWeight = FontWeight.SemiBold)
            Text(
                if (blockedApps.isEmpty()) {
                    "No apps are currently blocked."
                } else if (focusEnabled) {
                    "${blockedApps.size} apps are blocked through the local VPN."
                } else {
                    "${blockedApps.size} apps are marked blocked. Turn on Focus Mode to enforce it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            blockedApps.take(4).forEach { app ->
                Text(
                    "${app.appName} • ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (blockedApps.size > 4) {
                Text(
                    "+${blockedApps.size - 4} more blocked apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FocusSummary(
    checked: Boolean,
    blockedCount: Int,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Focus Mode", fontWeight = FontWeight.SemiBold)
                Text(
                    "$blockedCount blocked apps routed through local VPN.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun VpnStatus(
    granted: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (granted) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("VPN Permission", fontWeight = FontWeight.SemiBold)
                Text("Required to block apps on your device.", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onGrantClick) { Text(if (granted) "Granted" else "Grant") }
        }
    }
}

@Composable
private fun AppRuleRow(
    item: FocusAppItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppBrandIcon(item.appName, item.packageName)
            Column(Modifier.weight(1f)) {
                Text(item.appName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${item.accessLabel()} • ${item.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun EmptyFocusSearchState(filter: FocusFilter) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("No ${filter.label.lowercase()} apps found", fontWeight = FontWeight.SemiBold)
            Text(
                "Try another app name or package name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class FocusFilter(val label: String) {
    All("All"),
    Blocked("Blocked"),
    Allowed("Allowed");

    fun matches(item: FocusAppItem): Boolean = when (this) {
        All -> true
        Blocked -> item.isBlocked
        Allowed -> !item.isBlocked
    }

    fun count(items: List<FocusAppItem>): Int = items.count(::matches)
}

private fun FocusAppItem.accessLabel(): String =
    if (isBlocked) "Internet blocked" else "Internet allowed"

@Composable
private fun AppBrandIcon(appName: String, packageName: String) {
    val modifier = Modifier.size(40.dp)
    val systemIcon = rememberInstalledAppIcon(listOf(packageName))
    if (systemIcon != null) {
        Image(
            bitmap = systemIcon,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            appName.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun rememberInstalledAppIcon(packageNames: List<String>): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val iconSize = with(LocalDensity.current) { 40.dp.roundToPx() }
    val cacheKey = remember(packageNames, iconSize) { "${packageNames.joinToString()}@$iconSize" }

    val icon by produceState<ImageBitmap?>(
        initialValue = AppIconCache.icons[cacheKey],
        key1 = cacheKey
    ) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            packageNames.firstNotNullOfOrNull { packageName ->
                runCatching {
                    context.packageManager
                        .getApplicationIcon(packageName)
                        .toBitmap(width = iconSize, height = iconSize)
                        .asImageBitmap()
                }.getOrNull()
            }?.also { AppIconCache.icons[cacheKey] = it }
        }
    }
    return icon
}

private object AppIconCache {
    val icons = ConcurrentHashMap<String, ImageBitmap>()
}

private fun Context.startFocusVpn(blockedPackages: List<String>) {
    if (blockedPackages.isEmpty()) {
        stopFocusVpn()
        return
    }
    startService(
        Intent(this, FocusVpnService::class.java)
            .putStringArrayListExtra(
                FocusVpnService.EXTRA_BLOCKED_PACKAGES,
                ArrayList(blockedPackages)
            )
    )
}

private fun Context.stopFocusVpn() {
    stopService(Intent(this, FocusVpnService::class.java))
}
