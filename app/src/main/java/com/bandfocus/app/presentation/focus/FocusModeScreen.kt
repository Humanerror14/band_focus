package com.bandfocus.app.presentation.focus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.bandfocus.app.service.FocusVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
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
    val filteredApps = remember(search, selectedFilter, uiState.apps) {
        uiState.apps.filter { item ->
            val matchesSearch = search.isBlank() ||
                item.appName.contains(search, ignoreCase = true) ||
                item.packageName.contains(search, ignoreCase = true) ||
                item.accessLabel().contains(search, ignoreCase = true)
            matchesSearch && selectedFilter.matches(item)
        }
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
    val onFocusToggle: (Boolean) -> Unit = { enabled ->
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
    val onGrantVpnClick = {
        val permissionIntent = VpnService.prepare(context)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            vpnGranted = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val twoColumns = maxWidth >= 840.dp
        val appRows = remember(filteredApps, twoColumns) {
            if (twoColumns) filteredApps.chunked(2) else filteredApps.map { listOf(it) }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Focus Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            if (twoColumns) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        FocusSummary(
                            checked = focusEnabled,
                            blockedCount = blockedPackages.size,
                            onCheckedChange = onFocusToggle,
                            modifier = Modifier.weight(1f)
                        )
                        VpnStatus(
                            granted = vpnGranted,
                            onGrantClick = onGrantVpnClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item {
                    FocusSummary(
                        checked = focusEnabled,
                        blockedCount = blockedPackages.size,
                        onCheckedChange = onFocusToggle
                    )
                }
                item {
                    VpnStatus(
                        granted = vpnGranted,
                        onGrantClick = onGrantVpnClick
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search apps, package, or blocked") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            if (blockedApps.isNotEmpty()) {
                item {
                    BlockedInternetCard(
                        blockedApps = blockedApps,
                        focusEnabled = focusEnabled
                    )
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.label} (${filter.count(uiState.apps)})", maxLines = 1) }
                        )
                    }
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.applyPreset(preset) },
                            label = { Text(preset.label, maxLines = 1) }
                        )
                    }
                }
            }
            if (filteredApps.isEmpty()) {
                item { EmptyFocusSearchState(selectedFilter) }
            } else {
                items(
                    items = appRows,
                    key = { row -> row.joinToString(separator = "|") { it.packageName } }
                ) { row ->
                    if (twoColumns) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { item ->
                                AppRuleRow(
                                    item = item,
                                    checked = item.isBlocked,
                                    onCheckedChange = { checked -> viewModel.setBlocked(item, checked) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Box(Modifier.weight(1f))
                        }
                    } else {
                        val item = row.first()
                        AppRuleRow(
                            item = item,
                            checked = item.isBlocked,
                            onCheckedChange = { checked -> viewModel.setBlocked(item, checked) }
                        )
                    }
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (focusEnabled) Color(0xFFDCFCE7) else Color(0xFFFFF7ED)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    blockedApps.size.coerceAtMost(99).toString(),
                    color = if (focusEnabled) Color(0xFF166534) else Color(0xFF9A3412),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (focusEnabled) "Blocking active" else "Blocked list ready",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    blockedApps.take(3).joinToString { it.appName } +
                        if (blockedApps.size > 3) " +${blockedApps.size - 3} more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                blockedApps.take(3).forEach { app ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        MiniAppBadge(app.appName, app.packageName)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAppBadge(appName: String, packageName: String) {
    val modifier = Modifier.size(32.dp)
    val systemIcon = rememberInstalledAppIcon(listOf(packageName), iconSizeDp = 32)
    if (systemIcon != null) {
        Image(
            bitmap = systemIcon,
            contentDescription = null,
            modifier = modifier.clip(CircleShape)
        )
        return
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            appName.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
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
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
private fun rememberInstalledAppIcon(
    packageNames: List<String>,
    iconSizeDp: Int = 40
): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val iconSize = with(LocalDensity.current) { iconSizeDp.dp.roundToPx() }
    val cacheKey = remember(packageNames, iconSize) { "${packageNames.joinToString()}@$iconSize" }

    val icon by produceState<ImageBitmap?>(
        initialValue = AppIconCache.get(cacheKey),
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
            }?.also { AppIconCache.put(cacheKey, it) }
        }
    }
    return icon
}

private object AppIconCache {
    private const val MAX_ICON_COUNT = 128
    private val icons = LruCache<String, ImageBitmap>(MAX_ICON_COUNT)

    @Synchronized
    fun get(key: String): ImageBitmap? = icons.get(key)

    @Synchronized
    fun put(key: String, icon: ImageBitmap) {
        icons.put(key, icon)
    }
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
