package com.bandfocus.app.presentation.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.data.download.DownloadEngine
import com.bandfocus.app.data.download.DownloadProgress
import com.bandfocus.app.data.network.HeaderAnalyzer
import com.bandfocus.app.domain.model.DownloadMetadata
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val headerAnalyzer: HeaderAnalyzer,
    private val downloadEngine: DownloadEngine,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = downloadEngine.activeDownloads
    private var userSelectedMode = false

    init {
        viewModelScope.launch {
            preferencesRepository.defaultMode.collect { mode ->
                _uiState.update { state ->
                    if (state.metadata == null && !userSelectedMode) state.copy(selectedMode = mode) else state
                }
            }
        }
    }

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

    fun selectMode(mode: DownloadMode) {
        userSelectedMode = true
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun startDownload() {
        val state = uiState.value
        val metadata = state.metadata ?: run {
            _uiState.update { it.copy(error = "Please analyze a URL first") }
            return
        }

        viewModelScope.launch {
            if (preferencesRepository.wifiOnly.first() && !isWifiConnected()) {
                _uiState.update { it.copy(error = "Wi-Fi only is enabled. Connect to Wi-Fi or disable it in Settings.") }
                return@launch
            }
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            downloadEngine.startDownload(
                url = metadata.url,
                fileName = metadata.fileName,
                fileSize = metadata.fileSize ?: 0L,
                supportsRange = metadata.supportsRange,
                mode = state.selectedMode
            ).onSuccess { downloadId ->
                _uiState.update { it.copy(isAnalyzing = false, activeDownloadId = downloadId, error = null) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isAnalyzing = false, error = throwable.message ?: "Failed to start download") }
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

data class HomeUiState(
    val url: String = "",
    val isAnalyzing: Boolean = false,
    val metadata: DownloadMetadata? = null,
    val selectedMode: DownloadMode = DownloadMode.BALANCED,
    val activeDownloadId: String? = null,
    val error: String? = null
)
