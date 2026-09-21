# POTENTIA 0.2.0-rc1.2

- Adds opt-in automatic pilot-session sync over HTTPS.
- Pilot sessions are persisted locally before any upload attempt.
- WorkManager queues uploads only when a network connection is available.
- Pending sessions remain available for retry and manual CSV/JSON export.
- Mode Pribadi never stores/uploads raw 47-item responses.
- Withdrawing from pilot mode stops automatic transfer of pending sessions.
- Adds sync status and manual `Sinkronkan sekarang` action under Privasi & Data Pilot.
- Bumps consent version because the data-flow disclosure changed from manual-only export to optional configured automatic upload.
- Adds Google Apps Script receiver that writes live data to Google Sheets and maintains a CSV backup in Drive.
- Keeps assessment-history CSV export for Mode Pribadi and Pilot Pseudonim.
