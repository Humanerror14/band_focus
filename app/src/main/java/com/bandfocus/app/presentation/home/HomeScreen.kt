package com.bandfocus.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val metadata = uiState.metadata

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("BandFocus", style = MaterialTheme.typography.headlineLarge)
        Text("Focus your bandwidth. Download smarter.", style = MaterialTheme.typography.bodyLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = viewModel::onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Paste your download link") },
                    singleLine = true
                )
                Button(
                    onClick = viewModel::analyzeUrl,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isAnalyzing
                ) {
                    if (uiState.isAnalyzing) {
                        CircularProgressIndicator()
                    } else {
                        Text("Analyze URL")
                    }
                }
                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        Text("Mode", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val selected = uiState.selectedMode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.selectMode(com.bandfocus.app.domain.model.DownloadMode.ECO) },
                    label = { Text(if (selected == com.bandfocus.app.domain.model.DownloadMode.ECO) "Eco (selected)" else "Eco") }
                )
                AssistChip(
                    onClick = { viewModel.selectMode(com.bandfocus.app.domain.model.DownloadMode.BALANCED) },
                    label = { Text(if (selected == com.bandfocus.app.domain.model.DownloadMode.BALANCED) "Balanced (selected)" else "Balanced") }
                )
                AssistChip(
                    onClick = { viewModel.selectMode(com.bandfocus.app.domain.model.DownloadMode.TURBO) },
                    label = { Text(if (selected == com.bandfocus.app.domain.model.DownloadMode.TURBO) "Turbo (selected)" else "Turbo") }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Smart Diagnosis", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(metadata?.diagnosis ?: "Analyze a URL to see server support, file size, and recommended mode.")
                metadata?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("File: ${it.fileName}")
                    Text("Size: ${it.fileSize?.let(::formatBytes) ?: "Unknown"}")
                    Text("Range: ${if (it.supportsRange) "Supported" else "Not supported"}")
                    Text("Type: ${it.mimeType ?: "Unknown"}")
                }
            }
        }

        metadata?.let {
            Button(
                onClick = viewModel::startDownload,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isAnalyzing
            ) {
                Text("Start Download")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Active Download", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("No active download.")
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return "%.2f MB".format(mb)
}
