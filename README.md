# POTENTIA Android — Core Assessment V2

POTENTIA adalah aplikasi eksplorasi potensi berbasis Kotlin + Jetpack Compose. UI prototype awal tetap dipertahankan, tetapi Core V2 sudah mengganti alur asesmen demo dengan item bank dan scoring nyata untuk tahap pilot/research.

## Yang sudah berfungsi

- Splash dan first-run onboarding.
- Navigasi utama Home / Assessment / Growth / Profile.
- Item bank **47 item V4.1**:
  - Penalaran Logis: 10
  - Kreatif: 3
  - Verbal: 8
  - Spasial: 8
  - Sosial: 10
  - Praktis: 8
- Soal objective, Likert, SJT, free-text, dan soal spasial bergambar.
- Jawaban tersimpan selama sesi dan tetap tersedia saat pengguna mundur ke soal sebelumnya.
- Scoring deterministic untuk Logical, Verbal, Spatial, Social, dan Practical.
- Creative scoring menggunakan model **TF-IDF word+char + Ridge** secara on-device.
- Processing dilakukan di background thread.
- Result, radar chart, potential map, dan detail dimensi membaca hasil asesmen aktual — bukan `DemoScores`.
- Riwayat hasil tersimpan lokal dengan SharedPreferences (maksimum 20 sesi).
- Comparison memakai dua asesmen terbaru.
- Growth recommendation mengambil dimensi dengan indeks terendah dari hasil terbaru.
- Ekspor riwayat sebagai JSON melalui Android share sheet.
- Hapus riwayat memakai dialog konfirmasi.
- Preferensi Settings disimpan lokal.
- Android parity test untuk Creative scorer dan structural test untuk item bank.

## Status metodologi

**PILOT / RESEARCH ONLY.**

Item bank V4.1 belum tervalidasi secara psikometrik. Indeks 0–100 adalah transformasi scoring pilot, bukan percentile, norma populasi, IQ, diagnosis, atau ukuran kemampuan absolut.

Creative AI juga masih **experimental**. Model dilatih menggunakan respons Cambridge Alternate Uses Task berbahasa Inggris (`bowl`, `paperclip`), sedangkan prompt POTENTIA saat ini berbahasa Indonesia. Karena itu hasil Creative pada item POTENTIA ditandai out-of-domain dan belum boleh dipresentasikan sebagai skor kreativitas tervalidasi.

## Penyimpanan data

- Onboarding, preferences, dan history result: local SharedPreferences.
- Jawaban per-item hanya dipakai selama sesi dan tidak dipertahankan setelah scoring selesai.
- Creative AI berjalan on-device.
- Tidak ada backend atau sinkronisasi server pada versi ini.

## Lokasi aset penting

```text
app/src/main/assets/potentia_assessment/item_bank_v1_1.json
app/src/main/assets/potentia_assessment/assets/SPA_001.png ... SPA_008.png
app/src/main/assets/potentia_ai/creative_tfidf_ridge_v1.json
```

Core assessment:

```text
app/src/main/java/com/potentia/assessment/
├── AssessmentModels.kt
├── AssessmentRepository.kt
├── AssessmentScoringEngine.kt
└── AssessmentStorage.kt
```

Creative AI:

```text
app/src/main/java/com/potentia/ai/CreativeScorer.kt
```

## Tech

- Kotlin 1.9.24
- Android Gradle Plugin 8.5.2
- Jetpack Compose + Material 3
- Java 17
- minSdk 26
- targetSdk 35

## Run

Buka project di Android Studio, lakukan Gradle Sync, lalu Run pada emulator/perangkat.

Tidak ada API key atau backend yang diperlukan.
