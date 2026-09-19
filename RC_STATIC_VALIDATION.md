# POTENTIA RC Static Validation

Checks passed: **32/32**

- [x] AGP 8.7.3
- [x] Gradle wrapper 8.9
- [x] Unsupported compileSdk suppression removed
- [x] versionCode bumped
- [x] versionName RC
- [x] R8 enabled
- [x] resource shrinking enabled
- [x] No INTERNET permission
- [x] Backup disabled
- [x] Cleartext disabled
- [x] Launcher icon declared
- [x] Round icon declared
- [x] Keyboard adjustResize
- [x] Modern back callback enabled
- [x] All Android XML parses
- [x] 47 assessment items
- [x] Dimension counts 10/3/8/8/10/8
- [x] Creative model experimental
- [x] Creative model training tasks unchanged
- [x] 25 Creative parity vectors
- [x] DemoScores absent
- [x] Legacy Q_* flow absent
- [x] System BackHandler wired
- [x] Result return destination tracked
- [x] Dynamic Settings version label
- [x] Old Growth fake completion wording absent
- [x] Guided reflection present
- [x] Verified challenge present
- [x] Pilot export cache cleanup present
- [x] Full parity test annotated
- [x] Release readiness test present
- [x] .gitignore present

## Extra smoke tests

- Kotlin/JVM Growth catalog + reminder policy smoke: PASS (18 activities, 5 verified challenges).
- Kotlin/JVM deterministic scoring smoke: PASS.
- Android XML parse: PASS.
- JSON assets parse: PASS.

## Full Gradle build status

Not executed in this container because the Gradle wrapper cannot reach `services.gradle.org` (`UnknownHostException`). Android Studio/device remains the final compile/runtime gate.

Run on your machine:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

RC is **not** considered pilot-ready until those commands and the manual checklist in `QA_RELEASE_CANDIDATE.md` pass.
