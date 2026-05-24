package com.bandfocus.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.domain.model.DownloadStatus
import com.bandfocus.app.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: DownloadRepository
) : ViewModel() {
    val uiState: StateFlow<InsightsUiState> = repository.observeAll()
        .map { downloads ->
            val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
            val totalDownloadedBytes = completed.sumOf { task ->
                task.fileSize.takeIf { it > 0L } ?: task.downloadedBytes
            }
            val speeds = completed
                .mapNotNull { it.averageSpeed.takeIf { speed -> speed > 0L } }
                .takeLast(7)
            val averageSpeed = if (speeds.isNotEmpty()) speeds.average().toLong() else 0L

            InsightsUiState(
                totalDownloadedBytes = totalDownloadedBytes,
                averageSpeed = averageSpeed,
                totalDownloads = downloads.size,
                completedDownloads = completed.size,
                speedHistory = speeds
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightsUiState()
        )
}

data class InsightsUiState(
    val totalDownloadedBytes: Long = 0L,
    val averageSpeed: Long = 0L,
    val totalDownloads: Int = 0,
    val completedDownloads: Int = 0,
    val speedHistory: List<Long> = emptyList()
) {
    val successRate: Int =
        if (totalDownloads > 0) ((completedDownloads * 100f) / totalDownloads).toInt() else 0
}
