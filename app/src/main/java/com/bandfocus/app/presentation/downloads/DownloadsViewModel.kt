package com.bandfocus.app.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.data.download.DownloadEngine
import com.bandfocus.app.data.download.DownloadProgress
import com.bandfocus.app.domain.model.DownloadStatus
import com.bandfocus.app.domain.model.DownloadTask
import com.bandfocus.app.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val downloadEngine: DownloadEngine
) : ViewModel() {
    val uiState: StateFlow<DownloadsUiState> = combine(
        repository.observeAll(),
        downloadEngine.activeDownloads
    ) { downloads, activeProgress ->
        DownloadsUiState(
            downloads = downloads.map { task ->
                DownloadHistoryItem(
                    task = task,
                    progress = activeProgress[task.id]
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState()
    )

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadEngine.pauseDownload(id)
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            downloadEngine.cancelDownload(id)
        }
    }
}

data class DownloadsUiState(
    val downloads: List<DownloadHistoryItem> = emptyList()
)

data class DownloadHistoryItem(
    val task: DownloadTask,
    val progress: DownloadProgress?
) {
    val status: DownloadStatus = task.status
    val downloadedBytes: Long = progress?.downloadedBytes ?: task.downloadedBytes
    val totalBytes: Long = progress?.totalBytes?.takeIf { it > 0 } ?: task.fileSize
    val speed: Long = progress?.speed ?: task.averageSpeed
    val progressFraction: Float =
        if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
