# Core V2 Validation

Checks performed before packaging:

- V4.1 Android item bank JSON parses successfully.
- 47 active items found.
- Section counts: Logical 10, Creative 3, Verbal 8, Spatial 8, Social 10, Practical 8.
- All item IDs unique.
- All objective correct answers point to existing choices.
- All SJT score maps match their choice values.
- All 8 spatial assets exist.
- Android Creative model JSON exists.
- `CreativeScorer.kt` uses the Android-compatible regex (`\\b\\w\\w+\\b`) and does not contain the crashing `(?U)` flag.
- Core Kotlin classes (`CreativeScorer` + assessment models/repository/scoring/storage) passed a local Kotlin compiler smoke test using Android/JSON interface stubs.
- `PotentiaApp.kt` passed Kotlin syntax parsing (full Android/Compose compilation could not be run in this container because the Gradle wrapper distribution was not cached and network access to `services.gradle.org` is unavailable).
- No `DemoScores` references remain in the active `PotentiaApp.kt`.
- `MainActivity.kt` no longer contains the temporary Creative AI Logcat test.

Final verification to run in Android Studio:

1. Gradle Sync.
2. `:app:assembleDebug`.
3. Run app and complete at least one full 47-item assessment.
4. Verify Result -> Potential Map -> Detail -> History.
5. Complete a second assessment and verify Comparison.
6. Optionally run `CoreAssessmentInstrumentedTest` on an emulator/device.
