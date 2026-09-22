# POTENTIA 0.2.0-rc1.4 — Developer QA Fast Test

## Why
The developer may need to verify the end-to-end pilot pipeline before spending time manually answering all 47 assessment items.

## Added
- Hidden Developer QA mode for debug builds.
- Unlock by tapping `Pengaturan → Versi` seven times.
- `Privasi & Data Pilot → Developer QA → Jalankan Smoke Test 47 Item`.
- Generates 47 synthetic responses from the active item bank.
- Runs the real scoring path, including the on-device Creative scorer.
- Uploads with `dataKind=qa_test`.
- Apps Script routes synthetic test data to `QA_Sessions` and `QA_Responses`.
- `QA_Dashboard` explicitly warns that QA data must never be used in pilot analysis.
- Optional synthetic QA CSV export.

## Safety / research integrity
- QA test sessions are NOT written to personal assessment history.
- QA test sessions are NOT written to local pilot-session storage.
- QA test sessions are NOT counted in the real `Dashboard`, `Sessions`, or `Responses` sheets.
- Real pilot CSV backups are not changed by QA uploads.

## Fixed
- Updated About/Privacy text to match the auto-sync behavior introduced in RC1.2+.
- Fixed an accidental duplicate `Card(` call in the Pilot Data screen.
