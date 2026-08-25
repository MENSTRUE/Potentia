# POTENTIA Android MVP

Android/Jetpack Compose implementation of the current POTENTIA Figma Make prototype.

## Current scope

Implemented in this Android prototype:

- Splash
- First-run onboarding
- Home
- Assessment introduction
- Situational question
- Logic challenge
- Pattern challenge
- Spatial challenge
- Creative scenario
- Reflection question
- Processing screen
- Result overview
- Radar chart
- Potential detail
- Potential map
- Growth
- Assessment history
- Progress comparison
- Dimension library
- Profile
- Settings
- About assessment
- Bottom navigation
- First-run state with SharedPreferences

## Important prototype note

The current scores are UI/demo values only:

- Penalaran Logis: 82
- Kreatif: 71
- Verbal: 65
- Spasial: 78
- Sosial: 58
- Praktis: 74

They are **not** a validated psychological or psychometric scoring system.

The assessment flow is currently implemented to demonstrate the product experience. A real scoring methodology / recommendation model should be designed and validated separately.

## First-run behavior

First install / cleared app data:

Splash -> Onboarding -> Home

Returning user:

Splash -> Home

Use **Settings -> Reset data prototipe** to reset local prototype state during development.

## Tech

- Kotlin 1.9.24
- Android Gradle Plugin 8.5.2
- Jetpack Compose
- Material 3
- minSdk 26
- targetSdk 35
- Java 17

## Run

Open the project folder in Android Studio, allow Gradle Sync, then Run.

No API key or backend is required for this UI prototype.
