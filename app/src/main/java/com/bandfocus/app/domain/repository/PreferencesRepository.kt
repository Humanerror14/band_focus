package com.bandfocus.app.domain.repository

import com.bandfocus.app.domain.model.DownloadMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val defaultMode: Flow<DownloadMode>
    val defaultThreadCount: Flow<Int>
    val wifiOnly: Flow<Boolean>
    val autoFocusMode: Flow<Boolean>
    val theme: Flow<String>

    suspend fun setDefaultMode(mode: DownloadMode)
    suspend fun setDefaultThreadCount(count: Int)
    suspend fun setWifiOnly(enabled: Boolean)
    suspend fun setAutoFocusMode(enabled: Boolean)
    suspend fun setTheme(theme: String)
}
