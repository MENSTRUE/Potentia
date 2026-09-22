# POTENTIA 0.2.0-rc1.3

## Pilot monitoring improvements

- Added private Google Sheet `Dashboard` generation in the Apps Script receiver.
- Dashboard tracks synced sessions, unique pseudonymous participants, response-row completeness, versions, last receipt time, and the latest sessions.
- Added separate live CSV backups for Sessions and Responses.
- Result screen now distinguishes synced vs pending pilot sessions.
- Pending pilot results expose `Sinkronkan Sekarang` while preserving local-first storage and manual CSV backup.
- History now labels each assessment as PRIBADI, PILOT · LOKAL, PILOT · MENUNGGU, or PILOT · TERSINKRON.
- Version bumped to 0.2.0-rc1.3 / versionCode 5.

No psychometric validity claim is introduced by these monitoring features.
