package com.bandfocus.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.data.download.DownloadEngine
import com.bandfocus.app.data.network.HeaderAnalyzer
import com.bandfocus.app.domain.model.DownloadMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val headerAnalyzer: HeaderAnalyzer,
    private val downloadEngine: DownloadEngine
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url, error = null) }
    }

    fun analyzeUrl() {
        val url = uiState.value.url.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "URL cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            headerAnalyzer.analyze(url)
                .onSuccess { metadata ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            metadata = metadata,
                            selectedMode = metadata.recommendedMode,
                            error = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isAnalyzing = false, error = throwable.message ?: "Failed to analyze URL")
                    }
                }
        }
    }

    fun selectMode(mode: com.bandfocus.app.domain.model.DownloadMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun startDownload() {
        val state = uiState.value
        val metadata = state.metadata ?: run {
            _uiState.update { it.copy(error = "Please analyze a URL first") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            downloadEngine.startDownload(
                url = metadata.url,
                fileName = metadata.fileName,
                fileSize = metadata.fileSize ?: 0L,
                supportsRange = metadata.supportsRange,
                mode = state.selectedMode
            ).onSuccess { downloadId ->
                _uiState.update { it.copy(isAnalyzing = false, error = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isAnalyzing = false, error = throwable.message ?: "Failed to start download") }
            }
        }
    }
}

data class HomeUiState(
    val url: String = "",
    val isAnalyzing: Boolean = false,
    val metadata: DownloadMetadata? = null,
    val selectedMode: com.bandfocus.app.domain.model.DownloadMode = com.bandfocus.app.domain.model.DownloadMode.BALANCED,
    val error: String? = null
)
