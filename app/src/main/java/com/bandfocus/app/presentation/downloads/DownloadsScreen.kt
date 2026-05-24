package com.bandfocus.app.presentation.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bandfocus.app.domain.model.DownloadStatus

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by rememberSaveable { mutableStateOf(DownloadFilter.All) }
    val visibleDownloads = remember(selectedFilter, uiState.downloads) {
        uiState.downloads.filter { selectedFilter.matches(it.status) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val twoColumns = maxWidth >= 840.dp
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Search, contentDescription = "Search downloads", tint = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DownloadFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            if (visibleDownloads.isEmpty()) {
                item { EmptyDownloadsState(selectedFilter) }
            } else if (twoColumns) {
                val rows = visibleDownloads.chunked(2)
                items(rows, key = { row -> row.joinToString { it.task.id } }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { item ->
                            DownloadItem(
                                item = item,
                                onPause = { viewModel.pauseDownload(item.task.id) },
                                onCancel = { viewModel.cancelDownload(item.task.id) },
                                onDelete = { viewModel.deleteDownload(item.task.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
            } else {
                items(visibleDownloads, key = { it.task.id }) { item ->
                    DownloadItem(
                        item = item,
                        onPause = { viewModel.pauseDownload(item.task.id) },
                        onCancel = { viewModel.cancelDownload(item.task.id) },
                        onDelete = { viewModel.deleteDownload(item.task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    item: DownloadHistoryItem,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = item.status.visual()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.task.fileName,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.status.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = visual.color,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LinearProgressIndicator(progress = { item.progressFraction }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        formatSpeed(item.speed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (item.status == DownloadStatus.DOWNLOADING) {
                    IconButton(onClick = onPause) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause download")
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel download")
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete download")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsState(filter: DownloadFilter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("No ${filter.label.lowercase()} downloads", fontWeight = FontWeight.SemiBold)
            Text(
                "Analyze a URL on Home and start a download to see it here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private enum class DownloadFilter(val label: String) {
    All("All"),
    Active("Active"),
    Completed("Completed"),
    Failed("Failed"),
    Paused("Paused");

    fun matches(status: DownloadStatus): Boolean = when (this) {
        All -> true
        Active -> status == DownloadStatus.DOWNLOADING || status == DownloadStatus.QUEUED || status == DownloadStatus.ANALYZING
        Completed -> status == DownloadStatus.COMPLETED
        Failed -> status == DownloadStatus.FAILED || status == DownloadStatus.CANCELED
        Paused -> status == DownloadStatus.PAUSED
    }
}

private data class DownloadVisual(val icon: ImageVector, val color: Color)

private fun DownloadStatus.visual(): DownloadVisual = when (this) {
    DownloadStatus.COMPLETED -> DownloadVisual(Icons.Default.CheckCircle, Color(0xFF22C55E))
    DownloadStatus.FAILED, DownloadStatus.CANCELED -> DownloadVisual(Icons.Default.Error, Color(0xFFEF4444))
    DownloadStatus.PAUSED -> DownloadVisual(Icons.Default.Pause, Color(0xFFF59E0B))
    DownloadStatus.DOWNLOADING -> DownloadVisual(Icons.Default.PlayArrow, Color(0xFF2563EB))
    DownloadStatus.QUEUED, DownloadStatus.ANALYZING -> DownloadVisual(Icons.Default.HourglassTop, Color(0xFF06B6D4))
}

private fun DownloadStatus.label(): String = when (this) {
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.ANALYZING -> "Analyzing"
    DownloadStatus.DOWNLOADING -> "Downloading"
    DownloadStatus.PAUSED -> "Paused"
    DownloadStatus.COMPLETED -> "Completed"
    DownloadStatus.FAILED -> "Failed"
    DownloadStatus.CANCELED -> "Canceled"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "%.1f %s".format(value, units[unitIndex])
    }
}

private fun formatSpeed(bytesPerSecond: Long): String =
    if (bytesPerSecond > 0L) "${formatBytes(bytesPerSecond)}/s" else "-"
