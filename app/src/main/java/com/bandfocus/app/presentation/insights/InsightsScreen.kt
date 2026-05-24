package com.bandfocus.app.presentation.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val twoColumns = maxWidth >= 840.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MetricCard("Total Downloaded", formatBytes(uiState.totalDownloadedBytes), Icons.Default.DownloadDone)
                        MetricCard("Success Rate", "${uiState.successRate}%", Icons.AutoMirrored.Filled.TrendingUp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MetricCard("Average Speed", formatSpeed(uiState.averageSpeed), Icons.Default.Speed)
                        MetricCard("Total Downloads", uiState.totalDownloads.toString(), Icons.Default.Assessment)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Total", formatBytes(uiState.totalDownloadedBytes), Icons.Default.DownloadDone, Modifier.weight(1f))
                    MetricCard("Speed", formatSpeed(uiState.averageSpeed), Icons.Default.Speed, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Success", "${uiState.successRate}%", Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                    MetricCard("Downloads", uiState.totalDownloads.toString(), Icons.Default.Assessment, Modifier.weight(1f))
                }
            }
            SpeedChart(uiState.speedHistory)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Based on local download history", style = MaterialTheme.typography.bodySmall, color = Color(0xFF16A34A))
        }
    }
}

@Composable
private fun SpeedChart(speedHistory: List<Long>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Speed Over Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (speedHistory.isEmpty()) {
                Text(
                    "No completed downloads with speed data yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val maxSpeed = speedHistory.maxOrNull()?.takeIf { it > 0L } ?: 1L
                    val points = speedHistory.map { speed ->
                        (speed.toFloat() / maxSpeed).coerceIn(0.08f, 1f)
                    }
                    val step = size.width / (points.lastIndex.coerceAtLeast(1))
                    val offsets = points.mapIndexed { index, value ->
                        Offset(index * step, size.height - value * size.height)
                    }
                    for (i in 0 until offsets.lastIndex) {
                        drawLine(
                            color = Color(0xFF2563EB),
                            start = offsets[i],
                            end = offsets[i + 1],
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                    offsets.forEach { drawCircle(color = Color(0xFF06B6D4), radius = 7f, center = it) }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
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
    if (bytesPerSecond > 0L) "${formatBytes(bytesPerSecond)}/s" else "0 B/s"
