# POTENTIA Developer QA Fast Test

Use this only to verify the technical pipeline. Synthetic QA rows are not participant data and must not be analyzed as pilot responses.

## 1. Update Apps Script first
Copy the RC1.4 `tools/pilot_sync_apps_script/Code.gs` into the existing Apps Script project.
Run:

`setupPotentia()`

Then deploy a new version of the existing Web App deployment.

The spreadsheet should now contain:
- Dashboard
- Sessions
- Responses
- QA_Dashboard
- QA_Sessions
- QA_Responses

## 2. Build a debug APK
Build → Generate App Bundles or APKs → Generate APKs.

## 3. Unlock Developer QA
Open POTENTIA:

`Profil → Pengaturan → tap Versi 7x`

The version row should show `QA aktif`.

## 4. Run the smoke test
Open:

`Profil → Privasi & Data Pilot → Developer QA → Jalankan Smoke Test 47 Item`

Expected app message:

`BERHASIL · 47 respons QA dikirim...`

Then verify:
- `QA_Sessions` gets 1 new row.
- `QA_Responses` gets 47 new rows.
- `QA_Dashboard` updates.
- Real `Sessions`, `Responses`, and `Dashboard` do not change.

## 5. Test CSV export
After a QA run, tap `Ekspor CSV QA Sintetis`.
This checks FileProvider/share-sheet export without creating a real pilot session.

## Important
This fast test does NOT replace one final real end-to-end manual assessment before distributing the APK to pilot participants. The manual run is still needed to check wording, navigation, input behavior, resume/recovery, and the actual completion flow.
