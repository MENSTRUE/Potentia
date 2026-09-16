# POTENTIA Core V2 — Integration Notes

## Milestone ini

Core V2 membuat satu asesmen end-to-end benar-benar fungsional:

`47 items -> responses -> scoring -> Creative AI -> result -> history -> comparison -> growth`

Tidak ada lagi `DemoScores` untuk Result/Potential/History/Comparison.

## Scoring

- Logical / Verbal / Spatial: objective correctness.
- Social Likert: skala 1–5 dengan reverse scoring sesuai item bank.
- Social SJT / Practical SJT: option score dinormalisasi terhadap skor maksimum item.
- Creative: on-device TF-IDF + Ridge, lalu dirata-ratakan untuk respons pada item Creative.
- Dimension score hanya ditampilkan jika minimal 75% item pada dimensi memiliki respons valid.

## Important

Creative tetap experimental + out-of-domain untuk prompt POTENTIA Indonesia. Jangan menghapus warning user-facing sampai ada data respons Indonesia dengan human ratings, rater agreement, retraining/calibration, dan held-out evaluation.

## Test yang disiapkan

`app/src/androidTest/java/com/potentia/CoreAssessmentInstrumentedTest.kt`

Mengecek:

1. item bank memuat 47 item dengan distribusi 10/3/8/8/10/8;
2. Creative scorer Android menghasilkan parity vector yang sudah terbukti (`bowl / put salads in it`).

## Next setelah Core V2 stabil

- Draft/resume assessment lintas process death.
- Room database jika history/analytics akan diperluas.
- Profile editing.
- Notification scheduling yang benar untuk weekly reminder.
- Pilot response collection/export dengan consent dan participant/session IDs.
- Human review + usability pilot, lalu psychometric/pilot analysis.
