# POTENTIA 0.2.0-rc1.1

Pilot export UX update.

- Adds **Ekspor CSV Sesi Ini** directly on a pilot result screen.
- Per-session export contains only the selected pilot session.
- Keeps the existing **Profil → Privasi & Data → Ekspor seluruh sesi (CSV)** flow.
- Clarifies long-format semantics: one item response per CSV row; ~47 rows from one 47-item assessment still represent one participant/session.
- Filenames include a shortened pseudonymous session token and timestamp.
- No automatic upload or INTERNET permission is introduced.
- Adds `PILOT_EXPORT_GUIDE.md` for collection workflow and staged pilot-size guidance.
