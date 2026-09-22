# POTENTIA RC1.3 — Auto Pilot Sync + Live Monitor Setup

POTENTIA keeps completed Pilot Pseudonim data locally first, then queues automatic HTTPS upload with WorkManager when network is available.

The Android APK does not contain a Google account credential. A Google Apps Script web app acts as the receiver and writes into a Google Sheet in the researcher's Drive folder.

## Target Drive folder

The supplied Apps Script is configured for this Drive folder ID:

`1sfTUFFtuy0p9pqt5X8AaCV6BqTJe_2eF`

Change `TARGET_FOLDER_ID` in `tools/pilot_sync_apps_script/Code.gs` if the destination changes.

## 1. Create / update the Apps Script endpoint

1. Open `https://script.google.com` and create a project, or open the existing POTENTIA pilot sync project.
2. Replace the whole `Code.gs` with `tools/pilot_sync_apps_script/Code.gs` from this RC1.3 project.
3. Run `setupPotentia()` once from the Apps Script editor.
4. Approve Drive / Sheets permissions if Google asks.
5. Open **Execution log** and copy:
   - `Spreadsheet URL`
   - `UPLOAD_TOKEN`
6. Choose **Deploy → Manage deployments**.
7. If this is the first deployment, choose **New deployment → Web app**. If it already exists, edit the deployment and deploy a **new version**.
8. Execute as: **Me**.
9. Access: select the option that lets the APK call the endpoint without requiring each tester to sign in to your Google account (commonly displayed as `Anyone`).
10. Copy the URL ending in `/exec`.

The Google Sheet now contains:

- `Dashboard` — live operational monitor.
- `Sessions` — one row per accepted completed pilot session.
- `Responses` — long format, usually about 47 rows per session.

The Drive folder also keeps two CSV backups updated after accepted uploads:

- `potentia_pilot_sessions_live.csv`
- `potentia_pilot_responses_live.csv`

## 2. Configure the Android build

Keep your existing `sdk.dir` in the root `local.properties`, then add:

```properties
POTENTIA_PILOT_SYNC_ENDPOINT=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
POTENTIA_PILOT_SYNC_TOKEN=PASTE_TOKEN_FROM_SETUP_HERE
```

Do not commit `local.properties`.

Then:

1. Sync Gradle.
2. Clean Project.
3. Rebuild Project.
4. Generate a new APK.

The endpoint and token are embedded into the APK at build time. Rebuild the APK whenever either value changes.

## 3. Pilot flow

- `Mode Pribadi`: raw 47-item responses are not retained as pilot data and are not uploaded.
- `Pilot Pseudonim`: raw responses are stored locally first.
- WorkManager uploads pending pilot sessions when internet is available.
- Failed network attempts remain queued for retry.
- Manual CSV/JSON export remains available as backup.
- Withdrawing to Mode Pribadi stops automatic transfer of pending data.

## 4. What the tester sees

In **Riwayat Asesmen**, each result now shows one of:

- `PRIBADI`
- `PILOT · LOKAL`
- `PILOT · MENUNGGU`
- `PILOT · TERSINKRON`

On a pilot result screen:

- synced session → `Data pilot berhasil tersinkron`;
- pending session → `Sinkronkan Sekarang` is available;
- manual `Ekspor CSV Sesi Ini` remains available.

## 5. Check the end-to-end pipeline

1. Install the newly rebuilt APK.
2. Open **Profil → Privasi & Data Pilot**.
3. Under `Sinkronisasi pilot`, confirm **Auto-sync tersedia**.
4. Choose **Pilot Pseudonim**.
5. Complete one new 47-item assessment.
6. Keep internet enabled or reconnect after completion.
7. Re-open Result / Riwayat and check for `PILOT · TERSINKRON` after the worker succeeds.
8. Open the Google Sheet and verify:
   - Dashboard session count increases;
   - one new row appears in `Sessions`;
   - about 47 rows appear in `Responses`.
9. Open the Drive folder and verify both CSV backup files are present.
10. Repeat once with internet disabled during completion, then reconnect. The local session should remain pending and sync later.

## Security / research note

The embedded upload token is a lightweight anti-spam guard, not a high-security credential. Values embedded in a distributed APK can eventually be extracted. For a larger or production research deployment, use authenticated infrastructure, formal access controls, and a documented retention policy.

The Dashboard is an operational collection monitor only. Counts and session status are not psychometric validation, norms, diagnosis, or evidence of improvement.

## RC1.4 Developer QA
After replacing `Code.gs` with RC1.4, run `setupPotentia()` and deploy a new Web App version. The script will add `QA_Dashboard`, `QA_Sessions`, and `QA_Responses`. QA uploads are kept separate from real pilot sheets and CSV backups.
