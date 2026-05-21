package com.bandfocus.app.presentation.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        FilterChip(selected = true, onClick = {}, label = { Text("All") })
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Download history will appear here.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
