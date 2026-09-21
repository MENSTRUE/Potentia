# POTENTIA RC1.2 — Auto Pilot Sync Setup

POTENTIA can now keep pilot data locally **and** queue completed Pilot Pseudonim sessions for automatic HTTPS upload with WorkManager.

The Android app intentionally does not contain a Google account credential. A small Google Apps Script web app acts as the receiving endpoint and writes data into a Google Sheet in the researcher's Drive folder. The script also refreshes a CSV backup file after each newly accepted session.

## Target Drive folder

This project template is configured for this folder ID in the Apps Script source:

`1sfTUFFtuy0p9pqt5X8AaCV6BqTJe_2eF`

You can change `TARGET_FOLDER_ID` in `tools/pilot_sync_apps_script/Code.gs` later.

## 1. Create the Apps Script endpoint

1. Open https://script.google.com and create a new project.
2. Replace its `Code.gs` with `tools/pilot_sync_apps_script/Code.gs` from this project.
3. Run the `setupPotentia()` function once from the Apps Script editor.
4. Approve the requested Drive/Sheets permissions.
5. Open **Execution log**. Copy the generated `UPLOAD_TOKEN` and the created spreadsheet URL.
6. Choose **Deploy → New deployment → Web app**.
7. Execute as: **Me**.
8. Access: choose the option that allows the pilot APK to call the web app without requiring every tester to sign into your Google account (wording may appear as `Anyone`).
9. Deploy and copy the `/exec` web-app URL.

The generated Google Sheet contains:

- `Sessions` — one row per completed pilot session.
- `Responses` — long format, usually ~47 rows per completed session.

The script also maintains this file in the same Drive folder:

`potentia_pilot_responses_live.csv`

## 2. Configure the Android build

Open the project's root `local.properties`. Keep your existing `sdk.dir=...` line and add:

```properties
POTENTIA_PILOT_SYNC_ENDPOINT=https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec
POTENTIA_PILOT_SYNC_TOKEN=PASTE_TOKEN_FROM_SETUP_HERE
```

Do **not** commit `local.properties`.

Then:

1. Sync Gradle.
2. Clean Project.
3. Rebuild Project.
4. Generate the APK again.

The values are embedded into that APK at build time. If you edit `local.properties`, rebuild the APK before distributing it.

## 3. Pilot flow

- `Mode Pribadi`: no raw 47-item responses are uploaded.
- `Pilot Pseudonim`: completed raw responses are stored locally first.
- If auto-sync is configured and consent is still active, WorkManager queues the session.
- If the device is offline, the local record remains and the worker retries when network is available.
- Manual CSV/JSON export remains available as backup.
- Choosing Mode Pribadi / withdrawing stops automatic transfer of pending sessions.

Changing to auto-sync changes the research data flow, so the app uses a new consent version and existing pilot opt-ins are asked to decide again.

## 4. Check that it works

1. Install the newly rebuilt APK.
2. Open **Profil → Privasi & Data Pilot**.
3. `Sinkronisasi pilot` should say **Auto-sync tersedia**. If it says not configured, the endpoint/token was not embedded in the build.
4. Choose Pilot Pseudonim and complete one new assessment.
5. Wait for internet connectivity.
6. Check the Google Sheet. One new `Sessions` row and the item rows should appear.
7. Check the Drive folder for `potentia_pilot_responses_live.csv`.

## Security / research note

The upload token is an anti-spam guard, not a high-security secret: any value embedded in a distributed APK can eventually be extracted. For a larger or production research deployment, replace this lightweight endpoint with authenticated infrastructure and a formal data-retention/access policy.
