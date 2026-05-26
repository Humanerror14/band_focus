# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

BandFocus is a single-module Android app (`:app`) for smart download focus mode. It combines HTTPS download analysis, single/multi-thread downloading, Room-backed download history, DataStore preferences, foreground download service notifications, and a no-root Focus Mode built on Android `VpnService`.

The product requirements live in `prd_band_focus_smart_download_mode.md`. Treat that file as product direction, but verify current implementation in source before assuming a feature is complete.

## Common commands

Use the Gradle wrapper from the repository root.

- `./gradlew assembleDebug` — build a debug APK.
- `./gradlew installDebug` — install the debug APK on a connected emulator/device.
- `./gradlew testDebugUnitTest` — run local JVM unit tests.
- `./gradlew testDebugUnitTest --tests 'com.bandfocus.app.core.security.DownloadSecurityPolicyTest'` — run one JVM test class.
- `./gradlew testDebugUnitTest --tests 'com.bandfocus.app.core.security.DownloadSecurityPolicyTest.secureDownloadUrl_acceptsHttpsWithHost'` — run one JVM test method.
- `./gradlew connectedDebugAndroidTest` — run instrumented tests on a connected emulator/device when `app/src/androidTest/` exists.
- `./gradlew lintDebug` — run Android lint for the debug variant.
- `./gradlew assembleRelease` — build the minified/shrunk release variant.

## Architecture

The app follows a simple MVVM / clean-ish layering under `app/src/main/java/com/bandfocus/app/`:

- `presentation/` contains Compose screens and Hilt ViewModels. `presentation/navigation/BandFocusRoot.kt` owns the top-level destinations and switches between bottom navigation on compact widths and navigation rail on wider screens.
- `domain/` contains app models and repository interfaces used by ViewModels and services.
- `data/` contains Room entities/DAOs, repository implementations, HTTP header analysis, and the download engine.
- `core/` contains design/theme, dependency injection modules, notification IDs/channels, dispatchers, and security utilities.
- `service/` contains Android services for active downloads and Focus Mode VPN.

Dependency injection is Hilt-based. `core/di/AppModule.kt` provides the singleton `OkHttpClient`, Room database (`bandfocus.db`), DAOs, and DataStore (`bandfocus_prefs`). `RepositoryModule.kt` binds domain repository interfaces to `data/repository/*Impl` implementations.

## Download flow

`HomeViewModel` accepts a URL, calls `HeaderAnalyzer.analyze()`, and stores the resulting `DownloadMetadata` in UI state. `HeaderAnalyzer` currently requires secure `https://` URLs through `DownloadSecurityPolicy`, probes headers with `HEAD`, falls back to a ranged `GET bytes=0-0` when needed, extracts file metadata, detects range support, and recommends a download mode.

Starting a download goes through `DownloadEngine.startDownload()`. The engine creates a `DownloadTask`, persists it via `DownloadRepository`, starts `DownloadForegroundService`, and exposes live progress through `activeDownloads: StateFlow<Map<String, DownloadProgress>>`. If range is supported and the selected mode maps to more than one thread, it downloads chunks with HTTP `Range` requests into temporary part files, then merges them. Otherwise it downloads single-threaded. Progress is published about every 250 ms and persisted about every second.

`DownloadEngine.pauseDownload()` and `cancelDownload()` update the Room-backed task state and cancel active jobs, but resume behavior should be checked before relying on it for complete byte-range continuation.

## Persistence and UI state

Room stores downloads and Focus Mode app rules via `BandFocusDatabase`, `DownloadDao`, and `AppRuleDao`. Repository implementations map Room entities to domain models in `data/repository/`.

DataStore stores preferences such as default mode, Wi-Fi-only behavior, theme, and Focus Mode defaults through `PreferencesRepository`. ViewModels expose immutable `StateFlow` UI state to Compose.

## Focus Mode and Android services

`FocusVpnService` builds a local VPN tunnel for selected package names and drops packets read from the VPN interface. It requires Android VPN permission before start. Be careful when changing its allow/block semantics: Android `VpnService.Builder.addAllowedApplication()` routes only those apps through the VPN, so this implementation is using routed apps as the blocked set whose packets are dropped.

`DownloadForegroundService` is a minimal foreground service with an ongoing notification. Notification channel setup is in `core/notification/NotificationModule.kt`; manifest permissions and service declarations are in `app/src/main/AndroidManifest.xml`.

## Implementation notes

- The app is Kotlin + Jetpack Compose + Material 3, targeting Java/Kotlin 17, compile/target SDK 35, min SDK 26.
- The repository already has `AGENTS.md`; keep CLAUDE.md and AGENTS.md consistent when changing project-wide guidance.
- There is no README.md at the time this file was created.
- Current tests are under `app/src/test/java/`; no `app/src/androidTest/` tree exists yet.
- For UI changes, run the app on an emulator/device and exercise the changed flow when possible, especially navigation, permissions, VPN, notifications, and downloads.
