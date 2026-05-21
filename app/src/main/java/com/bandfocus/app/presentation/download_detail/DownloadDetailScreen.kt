package com.bandfocus.app.presentation.download_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadDetailScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Download Detail", style = MaterialTheme.typography.headlineMedium)
        Text("No active file selected.")
        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
        Text("Speed: — • ETA: — • Threads: —")
        Button(onClick = {}) { Text("Pause") }
        Button(onClick = {}) { Text("Cancel") }
    }
}
