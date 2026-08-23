# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a native Android app scaffolded from Android Studio's default "Empty Activity" (Jetpack Compose) template. The application ID/namespace is `com.joshuawallis.jwplayer`, but no MP3-player-specific functionality has been implemented yet — `MainActivity.kt` still contains the template's `Greeting("Android")` composable. Treat the existing code as a starting skeleton, not an established architecture to preserve.

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
- UI is built with Jetpack Compose (Material 3, BOM-managed via `libs.androidx.compose.bom`); `enableEdgeToEdge()` and `Scaffold` are set up in `MainActivity.kt` as the entry point.
- Compose theming lives under `app/src/main/java/com/joshuawallis/jwplayer/ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`).
- Kotlin/AGP/SDK versions: Kotlin 2.2.10, AGP 9.1.1, `compileSdk`/`targetSdk` 36, `minSdk` 34, Java 11 source/target compatibility.
