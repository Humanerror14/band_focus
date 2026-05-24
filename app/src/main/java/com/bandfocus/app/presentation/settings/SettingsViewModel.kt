package com.bandfocus.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.defaultMode,
        preferencesRepository.defaultThreadCount,
        preferencesRepository.wifiOnly,
        preferencesRepository.autoFocusMode,
        preferencesRepository.theme
    ) { defaultMode, defaultThreadCount, wifiOnly, autoFocusMode, theme ->
        SettingsUiState(
            defaultMode = defaultMode,
            defaultThreadCount = defaultThreadCount,
            wifiOnly = wifiOnly,
            autoFocusMode = autoFocusMode,
            theme = theme
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWifiOnly(enabled)
        }
    }

    fun setAutoFocusMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoFocusMode(enabled)
        }
    }

    fun setDefaultMode(mode: DownloadMode) {
        viewModelScope.launch {
            preferencesRepository.setDefaultMode(mode)
            preferencesRepository.setDefaultThreadCount(mode.defaultThreadCount())
        }
    }
}

data class SettingsUiState(
    val defaultMode: DownloadMode = DownloadMode.BALANCED,
    val defaultThreadCount: Int = 4,
    val wifiOnly: Boolean = false,
    val autoFocusMode: Boolean = false,
    val theme: String = "system"
)

private fun DownloadMode.defaultThreadCount(): Int = when (this) {
    DownloadMode.ECO -> 2
    DownloadMode.BALANCED -> 4
    DownloadMode.TURBO -> 8
    DownloadMode.NIGHT -> 2
}
