package com.bandfocus.app.presentation.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandfocus.app.domain.model.AppRule
import com.bandfocus.app.domain.repository.AppRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleRepository: AppRuleRepository
) : ViewModel() {
    private val installedApps = MutableStateFlow<List<InstalledAppPreview>>(emptyList())

    val uiState: StateFlow<FocusUiState> = combine(
        installedApps,
        appRuleRepository.observeAll()
    ) { apps, rules ->
        val rulesByPackage = rules.associateBy { it.packageName }
        FocusUiState(
            apps = apps.map { app ->
                val rule = rulesByPackage[app.packageName]
                FocusAppItem(
                    appName = app.appName,
                    packageName = app.packageName,
                    isBlocked = rule?.isBlockedInFocusMode ?: defaultBlocked(app),
                    isWhitelisted = rule?.isWhitelisted ?: defaultWhitelisted(app)
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState()
    )

    init {
        refreshInstalledApps()
    }

    fun setBlocked(app: FocusAppItem, blocked: Boolean) {
        viewModelScope.launch {
            appRuleRepository.upsert(
                AppRule(
                    packageName = app.packageName,
                    appName = app.appName,
                    isBlockedInFocusMode = blocked,
                    isWhitelisted = !blocked && app.isWhitelisted,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun applyPreset(preset: FocusPreset) {
        viewModelScope.launch {
            when (preset) {
                FocusPreset.BlockSocial -> uiState.value.apps
                    .filter { it.isSocialOrStreaming() }
                    .forEach { setBlocked(it, true) }

                FocusPreset.AllowMessaging -> uiState.value.apps
                    .filter { it.isMessaging() }
                    .forEach { setBlocked(it, false) }

                FocusPreset.Reset -> appRuleRepository.deleteAll()
            }
        }
    }

    private fun refreshInstalledApps() {
        viewModelScope.launch {
            installedApps.value = loadLaunchableApps()
        }
    }

    private suspend fun loadLaunchableApps(): List<InstalledAppPreview> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                InstalledAppPreview(
                    appName = info.loadLabel(packageManager).toString(),
                    packageName = packageName
                )
            }

        val knownFocusApps = (socialAndStreamingPackages + messagingPackages)
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    InstalledAppPreview(
                        appName = appInfo.loadLabel(packageManager).toString(),
                        packageName = packageName
                    )
                }.getOrNull()
            }

        (launchableApps + knownFocusApps)
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    private fun defaultBlocked(app: InstalledAppPreview): Boolean = app.isSocialOrStreaming()

    private fun defaultWhitelisted(app: InstalledAppPreview): Boolean = app.isMessaging()
}

data class FocusUiState(
    val apps: List<FocusAppItem> = emptyList()
)

data class FocusAppItem(
    val appName: String,
    val packageName: String,
    val isBlocked: Boolean,
    val isWhitelisted: Boolean
)

enum class FocusPreset(val label: String) {
    BlockSocial("Block social"),
    AllowMessaging("Allow messaging"),
    Reset("Reset")
}

private data class InstalledAppPreview(
    val appName: String,
    val packageName: String
)

private fun InstalledAppPreview.isSocialOrStreaming(): Boolean =
    packageName in socialAndStreamingPackages || appName.containsAny(socialAndStreamingNames)

private fun InstalledAppPreview.isMessaging(): Boolean =
    packageName in messagingPackages || appName.containsAny(messagingNames)

private fun FocusAppItem.isSocialOrStreaming(): Boolean =
    packageName in socialAndStreamingPackages || appName.containsAny(socialAndStreamingNames)

private fun FocusAppItem.isMessaging(): Boolean =
    packageName in messagingPackages || appName.containsAny(messagingNames)

private fun String.containsAny(keywords: Set<String>): Boolean =
    keywords.any { contains(it, ignoreCase = true) }

private val socialAndStreamingPackages = setOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.google.android.youtube",
    "com.netflix.mediaclient",
    "com.facebook.katana",
    "com.twitter.android",
    "com.x.android"
)

private val messagingPackages = setOf(
    "com.whatsapp",
    "org.telegram.messenger",
    "org.thunderdog.challegram"
)

private val socialAndStreamingNames = setOf("instagram", "tiktok", "youtube", "netflix", "facebook", "twitter")
private val messagingNames = setOf("whatsapp", "telegram", "messages", "signal")
