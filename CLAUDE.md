# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Lead Link** is an Android mobile app for FBLA (Future Business Leaders of America) members. It provides user profiles, connections, real-time chat, competition reminders, and news. Built with Kotlin + Jetpack Compose, backed by Firebase.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device
./gradlew installDebug

# Clean build
./gradlew clean build
```

## Test Commands

```bash
# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.example.myapplication.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Firebase Cloud Functions

```bash
cd functions

# Serve locally with Firebase emulator
npm run serve

# Deploy functions to production
npm run deploy

# Stream logs
npm run logs
```

## Architecture

### Android App (single-module)

The app is a **single-activity architecture** (`MainActivity.kt`) with all screens implemented as Jetpack Compose composables. Navigation is handled within the single activity.

`MainActivity.kt` (~8000 lines) contains nearly all UI composables. Key screens include authentication (`AuthScreen`, `TwoFactorVerificationScreen`), `ProfileScreen`, `SettingsScreen`, `TutorialScreen`, and various chat/connection/reminder screens.

**State and data flow:**
- Local preferences (theme, accessibility options, notification settings) are persisted via DataStore in `SettingsStore.kt` and accessed through a `StateFlow`-based API.
- Cloud data (profiles, connections, messages, reminders) goes through Firebase Firestore directly from composables.
- Profile images use Firebase Cloud Storage.
- Firebase Auth handles sign-in; 2FA codes are sent via `send2FAEmail` Cloud Function (SendGrid).

### Firebase Backend (`functions/index.js`)

Single exported Cloud Function: `send2FAEmail`. Called by the Android app during login to deliver verification codes via SendGrid email.

### Key Dependencies

Managed through the version catalog at `gradle/libs.versions.toml`:
- **Compose BOM:** 2026.01.00 (Material3)
- **Firebase BOM:** 33.9.0 (Auth, Firestore, Storage)
- **Coil 2.6.0** — image loading in composables
- **Jsoup 1.17.2** — web scraping for news/events
- **OkHttp 4.12.0** — HTTP client
- **DataStore 1.1.1** — local settings persistence

### Versions

- Kotlin: 2.2.10
- AGP: 9.0.1
- Min SDK: 24 (Android 7.0), Target SDK: 35, Compile SDK: 36
- Java compatibility: JDK 11
- Firebase project: `fbla-connect2026`
- App package: `com.example.myapplication`
