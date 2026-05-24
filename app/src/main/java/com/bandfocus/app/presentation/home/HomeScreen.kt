package com.bandfocus.app.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.width
import com.bandfocus.app.domain.model.DownloadMetadata
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bandfocus.app.R
import com.bandfocus.app.core.design.BandFocusLime
import com.bandfocus.app.data.download.DownloadProgress
import com.bandfocus.app.domain.model.DownloadMode

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val metadata = uiState.metadata
    val activeProgress = uiState.activeDownloadId?.let(activeDownloads::get)
    val clipboardManager = LocalClipboardManager.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val useTwoColumns = maxWidth >= 900.dp
        val gap = if (useTwoColumns) 18.dp else 14.dp

        if (useTwoColumns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    HomeHero(metadata = metadata, selectedMode = uiState.selectedMode)
                    UrlAnalyzerCard(
                        url = uiState.url,
                        isAnalyzing = uiState.isAnalyzing,
                        error = uiState.error,
                        onUrlChanged = viewModel::onUrlChanged,
                        onPaste = {
                            clipboardManager.getText()?.text?.let(viewModel::onUrlChanged)
                        },
                        onAnalyze = viewModel::analyzeUrl
                    )
                    ModeSelector(
                        selectedMode = uiState.selectedMode,
                        onSelectMode = viewModel::selectMode
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    SmartDiagnosisCard(metadata = metadata)
                    QuickStatsCard(metadata = metadata)
                    ActiveDownloadCard(metadata = metadata, progress = activeProgress)
                    if (metadata != null) {
                        Button(
                            onClick = viewModel::startDownload,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Focus Download")
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                HomeHero(metadata = metadata, selectedMode = uiState.selectedMode)
                UrlAnalyzerCard(
                    url = uiState.url,
                    isAnalyzing = uiState.isAnalyzing,
                    error = uiState.error,
                    onUrlChanged = viewModel::onUrlChanged,
                    onPaste = {
                        clipboardManager.getText()?.text?.let(viewModel::onUrlChanged)
                    },
                    onAnalyze = viewModel::analyzeUrl
                )
                ModeSelector(
                    selectedMode = uiState.selectedMode,
                    onSelectMode = viewModel::selectMode
                )
                SmartDiagnosisCard(metadata = metadata)
                QuickStatsCard(metadata = metadata)
                if (metadata != null) {
                    Button(
                        onClick = viewModel::startDownload,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Focus Download")
                    }
                }
                ActiveDownloadCard(metadata = metadata, progress = activeProgress)
            }
        }
    }
}

@Composable
private fun HomeHero(
    metadata: DownloadMetadata?,
    selectedMode: DownloadMode
) {
    val recommendedMode = metadata?.recommendedMode ?: selectedMode
    val heroBrush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    val fileLine = metadata?.fileName ?: "Ready for smart downloads"
    val detailLine = metadata?.let {
        "${recommendedMode.label()} mode • ${it.fileSize?.let(::formatBytes) ?: "Unknown size"}"
    } ?: "${recommendedMode.label()} mode selected • Analyze a URL to tune the download"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.bandfocus_logo),
                    contentDescription = "BandFocus logo",
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BandFocus",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        HeaderPill("${recommendedMode.label()} Mode")
                    }
                    Text(
                        text = fileLine,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderMetric(
                    label = "Range",
                    value = metadata?.let { if (it.supportsRange) "Ready" else "Single" } ?: "Scan",
                    modifier = Modifier.weight(1f)
                )
                HeaderMetric(
                    label = "Threads",
                    value = recommendedMode.defaultThreads(),
                    modifier = Modifier.weight(1f)
                )
                HeaderMetric(
                    label = "Mode",
                    value = recommendedMode.label(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun HeaderMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UrlAnalyzerCard(
    url: String,
    isAnalyzing: Boolean,
    error: String?,
    onUrlChanged: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Enter download URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paste your download link") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPaste, modifier = Modifier.weight(1f)) {
                    Text("Paste")
                }
                Button(
                    onClick = onAnalyze,
                    modifier = Modifier.weight(1f),
                    enabled = !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Analyze URL")
                    }
                }
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeSelector(
    selectedMode: DownloadMode,
    onSelectMode: (DownloadMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadMode.entries.forEach { mode ->
                    ModeCard(
                        mode = mode,
                        selected = selectedMode == mode,
                        onClick = { onSelectMode(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: DownloadMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) BandFocusLime.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) BandFocusLime else MaterialTheme.colorScheme.outlineVariant
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = container),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, SolidColor(borderColor)),
        modifier = Modifier
            .width(112.dp)
            .height(76.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(mode.icon(), contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(mode.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SmartDiagnosisCard(metadata: DownloadMetadata?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                Text("Smart Diagnosis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(metadata?.diagnosis ?: "Analyze a URL to see server support, file size, and recommended mode.", color = Color(0xFF14532D))
            metadata?.let {
                Spacer(Modifier.height(12.dp))
                Text("File: ${it.fileName}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF14532D))
                Text("Size: ${it.fileSize?.let(::formatBytes) ?: "Unknown"}", color = Color(0xFF14532D))
                Text("Range: ${if (it.supportsRange) "Supported" else "Not supported"}", color = Color(0xFF14532D))
                Text("Type: ${it.mimeType ?: "Unknown"}", color = Color(0xFF14532D))
            }
        }
    }
}

@Composable
private fun QuickStatsCard(metadata: DownloadMetadata?) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick stats", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBlock(
                    title = "File size",
                    value = metadata?.fileSize?.let(::formatBytes) ?: "—",
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                StatBlock(
                    title = "Ranges",
                    value = metadata?.let { if (it.supportsRange) "Yes" else "No" } ?: "—",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBlock(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    metadata: DownloadMetadata?,
    progress: DownloadProgress?
) {
    val totalBytes = progress?.totalBytes?.takeIf { it > 0 } ?: metadata?.fileSize ?: 0L
    val downloadedBytes = progress?.downloadedBytes ?: 0L
    val progressFraction = if (totalBytes > 0L) {
        (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Active Download", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                metadata?.fileName ?: "No active download.",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speed: ${progress?.speed?.let(::formatSpeed) ?: "—"}")
                    Text("ETA: ${progress?.eta?.let(::formatEta) ?: "—"}")
                    Text("Threads: ${metadata?.recommendedMode?.defaultThreads() ?: "—"}")
                }
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

private fun DownloadMode.icon(): ImageVector = when (this) {
    DownloadMode.ECO -> Icons.Default.Eco
    DownloadMode.BALANCED -> Icons.Default.Speed
    DownloadMode.TURBO -> Icons.Default.Bolt
    DownloadMode.NIGHT -> Icons.Default.Nightlight
}

private fun DownloadMode.defaultThreads(): String = when (this) {
    DownloadMode.ECO -> "2"
    DownloadMode.BALANCED -> "4"
    DownloadMode.TURBO -> "8"
    DownloadMode.NIGHT -> "2"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
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
    if (bytesPerSecond > 0L) "${formatBytes(bytesPerSecond)}/s" else "—"

private fun formatEta(seconds: Long): String {
    if (seconds <= 0L) return "—"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
}
