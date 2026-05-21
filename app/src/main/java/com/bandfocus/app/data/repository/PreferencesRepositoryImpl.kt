package com.bandfocus.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bandfocus.app.domain.model.DownloadMode
import com.bandfocus.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {
    override val defaultMode: Flow<DownloadMode> = dataStore.data.map { prefs ->
        prefs[DEFAULT_MODE_KEY] ?: DownloadMode.BALANCED.name
    }.map { name -> DownloadMode.valueOf(name) }

    override val defaultThreadCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DEFAULT_THREAD_COUNT_KEY] ?: 4
    }

    override val wifiOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[WIFI_ONLY_KEY] ?: false
    }

    override val autoFocusMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_FOCUS_MODE_KEY] ?: false
    }

    override val theme: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "system"
    }

    override suspend fun setDefaultMode(mode: DownloadMode) {
        dataStore.edit { prefs ->
            prefs[DEFAULT_MODE_KEY] = mode.name
        }
    }

    override suspend fun setDefaultThreadCount(count: Int) {
        dataStore.edit { prefs ->
            prefs[DEFAULT_THREAD_COUNT_KEY] = count
        }
    }

    override suspend fun setWifiOnly(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[WIFI_ONLY_KEY] = enabled
        }
    }

    override suspend fun setAutoFocusMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_FOCUS_MODE_KEY] = enabled
        }
    }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme
        }
    }
}

private val DEFAULT_MODE_KEY = stringPreferencesKey("default_mode")
private val DEFAULT_THREAD_COUNT_KEY = intPreferencesKey("default_thread_count")
private val WIFI_ONLY_KEY = booleanPreferencesKey("wifi_only")
private val AUTO_FOCUS_MODE_KEY = booleanPreferencesKey("auto_focus_mode")
private val THEME_KEY = stringPreferencesKey("theme")
