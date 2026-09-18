# POTENTIA Research Pilot Layer

This project build supports two explicit data modes for a new assessment session:

- **Personal mode**: only dimension-level assessment history is retained after scoring. Per-item responses are not retained after the assessment completes.
- **Pseudonymous research-pilot opt-in**: POTENTIA stores a random participant ID, random session ID, version metadata, timestamps, per-item responses (including free text), and dimension-level results locally on the device.

## Privacy properties

- No name, email, phone number, or device identifier is collected by the research-pilot storage layer.
- The Android manifest does not request `INTERNET` permission.
- Research-pilot records are not uploaded automatically.
- Research-pilot exports are created only after an explicit user action and are shared as JSON/CSV files through Android's share sheet.
- Android app backup is disabled because opted-in pilot records may contain raw free-text responses.
- Users can stop future pilot collection without deleting existing local records, or explicitly delete all local pilot records and the pseudonymous participant ID.

## Consent/version behavior

Current in-app consent version: `potentia-pilot-consent-v1`.

A research opt-in from an older consent version is not silently carried forward to new sessions. The user is asked to decide again before starting a new assessment.

Consent is snapshotted at assessment start. A session is exported to the pilot dataset only when:

1. the user was opted in when that session started, and
2. research participation is still active with the same current consent version when scoring completes.

This prevents retroactive collection after a user opts in halfway through a session and honors withdrawal before completion.

## Research status

The consent UI is an application transparency/control layer. It is **not** a replacement for institutional ethics review, formal informed-consent documents, recruitment procedures, or data-governance requirements that may apply to an actual research study.

The current 47-item assessment remains pilot/research only and is not psychometrically validated. The Creative scorer remains experimental and out-of-domain for the current Indonesian Potentia creative prompts.
