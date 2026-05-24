# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project. The `app/` module contains application code under `app/src/main/java/com/bandfocus/app/`, grouped by responsibility:

- `core/` for shared design, dependency injection, notifications, and utilities.
- `data/` for Room entities/DAOs, repositories, networking, and download infrastructure.
- `domain/` for models and repository interfaces.
- `presentation/` for Jetpack Compose screens, navigation, and ViewModels.
- `service/` for Android foreground and VPN services.

Android resources live in `app/src/main/res/`. Product/design references are in `design/`, and the requirements document is `prd_band_focus_smart_download_mode.md`.

## Build, Test, and Development Commands

Use the Gradle wrapper from the repository root:

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew installDebug` installs the debug build on a connected device or emulator.
- `./gradlew testDebugUnitTest` runs local JVM unit tests.
- `./gradlew connectedDebugAndroidTest` runs instrumented tests on a device or emulator.
- `./gradlew lintDebug` runs Android lint for the debug variant.

Use the wrapper instead of a system Gradle installation.

## Coding Style & Naming Conventions

The project uses Kotlin, Jetpack Compose, Hilt, Room, DataStore, and coroutines. Java/Kotlin bytecode targets version 17. Follow existing package boundaries and keep UI state in ViewModels as behavior grows.

Use 4-space indentation for Kotlin. Name composables with `PascalCase` nouns ending in `Screen`, `Root`, or a specific UI role when applicable, for example `HomeScreen`. Name ViewModels with the `ViewModel` suffix, Room DAOs with `Dao`, entities with `Entity`, and repository implementations with `Impl`.

## Testing Guidelines

Place JVM unit tests in `app/src/test/java/` and instrumented or Compose UI tests in `app/src/androidTest/java/`. Match the source package path and name test files after the class or behavior, such as `HomeViewModelTest`.

Prioritize repository behavior, Room conversions/queries, download mode rules, and ViewModel state transitions. Run `./gradlew testDebugUnitTest` before opening a pull request; run `connectedDebugAndroidTest` when changing services, permissions, navigation, or Compose UI.

## Commit & Pull Request Guidelines

History uses short, imperative commit messages with a trailing period, for example `Ignore local.properties explicitly.` Keep commits focused and describe the change.

Pull requests should include a summary, testing performed, linked issue or requirement when relevant, and screenshots or screen recordings for UI changes. Call out permission, VPN service, notification, database schema, or migration impacts.

## Security & Configuration Tips

Do not commit `local.properties`, build output, APKs, or reports. Keep secrets out of source and Gradle files. When adding Android permissions or services, document the user-facing reason and verify manifest changes carefully.
