# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a working native MP3 player app, not a scaffold — treat the existing code as an established architecture to preserve and extend, not a starting skeleton. A single `ComponentActivity` (`MainActivity.kt`) shows a 1.5s hand-rolled Compose splash screen, then renders `Mp3playerTheme` + `AppNavHost`. The app lets a user pick a root folder via the Storage Access Framework, browse it (folders/audio files), play tracks with a persistent mini-player, and optionally play a looping white-noise track independently selected in Settings.

## Commands

Build and test via the Gradle wrapper from the repo root (`./gradlew`, not a global `gradle`):

- Build debug APK: `./gradlew assembleDebug`
- Run unit tests (JVM, in `app/src/test`): `./gradlew testDebugUnitTest`
- Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.joshuawallis.jwplayer.ExampleUnitTest"`
- Run instrumented tests (require a connected device/emulator, in `app/src/androidTest`): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lint`
- Clean build: `./gradlew clean`

## Architecture

- Single Gradle module: `app` (see `settings.gradle.kts`).
- Dependency versions are centralized in `gradle/libs.versions.toml` (Gradle version catalog) and referenced from `app/build.gradle.kts` via `libs.*` aliases — add new dependencies there rather than hardcoding coordinates in the module build file.
- UI is built with Jetpack Compose (Material 3, BOM-managed via `libs.androidx.compose.bom`); `enableEdgeToEdge()` is set up in `MainActivity.kt` as the entry point, which also hosts the splash screen composable.
- **Navigation**: `androidx.navigation.compose`, two flat routes (`main`, `settings`) defined in `ui/navigation/NavGraph.kt`, which also owns root-folder/white-noise URI state (loaded from `SettingsRepository`) and intercepts the system back button to walk up a folder before leaving the screen.
- **Playback state**: `PlaybackViewModel` (`playback/PlaybackViewModel.kt`, an `AndroidViewModel`) is the sole source of truth for playback — one `ExoPlayer` (Media3) instance, exposed via `StateFlow<PlaybackUiState>`. `PlaybackMode` (`NONE`/`LIBRARY`/`WHITE_NOISE`) tracks whether the player is idle, playing a library track, or playing white noise; both modes share the same player instance, so starting one stops the other.
- **Folder browsing**: `ui/screens/main/LibraryBrowser.kt` holds current-folder state (hoisted, not a ViewModel), lists contents via `DirectoryLister` (object, `data/DirectoryLister.kt`), caches listings in a `ConcurrentHashMap<Uri, DirectoryListing>`, and prefetches one level of subfolders in the background on entry to a folder.
- **Persistence**: `SettingsRepository` (`data/SettingsRepository.kt`) is a thin `SharedPreferences` wrapper storing the root-folder URI and white-noise URI. No database, no DataStore.
- **File access**: Storage Access Framework only (`DocumentFile`/`Uri`/`ContentResolver`, persistable URI permissions) — never a raw `File` API, consistent with scoped storage.
- Compose theming lives under `app/src/main/java/com/joshuawallis/jwplayer/ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`); dark theme is forced on by default.
- Kotlin/AGP/SDK versions: Kotlin 2.2.10, AGP 9.1.1, `compileSdk`/`targetSdk` 36, `minSdk` 26, Java 11 source/target compatibility.

## Conventions to follow

- **File access is SAF-only.** Never use the raw `java.io.File` API — always go through `DocumentFile`/`Uri`/`ContentResolver`, matching every existing data-access path in this app.
- **`PlaybackViewModel`/`StateFlow` is the only source of truth for playback state.** Don't introduce a second, parallel place that tracks what's playing — extend the existing `PlaybackUiState`/`PlaybackMode` instead.
- **Accessibility is a tested requirement, not optional polish.** Interactive elements need `Modifier.semantics { contentDescription = ... }` (see the existing accessibility pass across `FolderListView.kt`, `MiniPlayer.kt`, `SettingsScreen.kt` for the established pattern/wording style).
- **No new DI framework or major library additions without being asked.** This app deliberately has no dependency-injection framework — dependencies are constructed manually (e.g. `remember { SettingsRepository(applicationContext) }`). Stick to that pattern rather than introducing Hilt/Koin/etc.
