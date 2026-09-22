# POTENTIA Android — Research Pilot RC

POTENTIA adalah aplikasi Android eksplorasi diri berbasis Kotlin + Jetpack Compose. Build ini adalah **research-pilot release candidate**, bukan alat diagnosis psikologis dan belum merupakan instrumen psikometrik tervalidasi.

## Core yang sudah terhubung

- 47 item V4.1: Logical 10, Creative 3, Verbal 8, Spatial 8, Social 10, Practical 8.
- Session recovery dengan ViewModel + draft lokal.
- Scoring deterministic untuk 5 dimensi dan Creative AI TF-IDF + Ridge on-device.
- Result, radar, detail, history, comparison, Growth V3, settings, weekly reminder, dan export.
- Growth V3 memakai Mini Challenge terverifikasi atau Guided Reflection; tidak menganggap klik tombol sebagai bukti kemampuan meningkat.
- Research pilot mode bersifat pseudonim, opt-in, lokal, tanpa backend dan tanpa permission INTERNET.
- Pilot export JSON/CSV dilakukan manual melalui Android share sheet.

## Status metodologi

**PILOT / RESEARCH ONLY.**

Indeks 0–100 adalah transformasi scoring pilot, bukan percentile, norma populasi, IQ, diagnosis, atau ukuran kemampuan absolut. Creative AI masih eksperimental dan out-of-domain untuk prompt Potentia Bahasa Indonesia karena model asal dilatih pada Cambridge AUT berbahasa Inggris (`bowl`, `paperclip`).

## Build / release

- Kotlin 1.9.24
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Java 17
- compileSdk / targetSdk 35
- minSdk 26
- version `0.2.0-rc1`
- Release build mengaktifkan R8 + resource shrinking.

Run checks sebelum pilot:

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

Release APK/AAB tetap perlu signing key milik developer; jangan commit keystore atau password ke repository.

## Privacy

- `android.permission.INTERNET`: tidak diminta.
- Android backup: dimatikan.
- Raw pilot responses hanya disimpan ketika user memilih mode pilot pseudonim.
- Export pilot bisa berisi free text; file sementara dibuat di cache dan export lama dibersihkan sebelum export baru.

Lihat `QA_RELEASE_CANDIDATE.md` untuk regression checklist dan known limitations.

## Developer QA (RC1.4)
Debug builds include a hidden smoke-test mode so the 47-item sync/export pipeline can be verified without manually completing the assessment. Tap `Pengaturan → Versi` seven times, then open `Privasi & Data Pilot`. QA data is routed to separate `QA_*` sheets and must never be included in pilot analysis. See `QA_FAST_TEST_GUIDE.md`.
