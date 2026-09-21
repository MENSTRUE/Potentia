# POTENTIA Pilot Export Guide — RC1.1

## Participant flow

1. Choose **Pilot Pseudonim** before starting a new assessment.
2. Complete the 47-item assessment.
3. On the result screen, use **Ekspor CSV Sesi Ini**.
4. Android opens the system share sheet. Save/send the CSV only to the approved research destination.
5. If the result screen has already been closed, use **Profil → Privasi & Data → Ekspor seluruh sesi (CSV)**.

## What one CSV means

POTENTIA exports data in long format. One row represents one item response.

Therefore, one participant completing one 47-item session will normally produce about 47 response rows plus one CSV header row. This is still **one participant / one assessment session**, not 47 respondents.

The CSV carries pseudonymous `participant_id` and `session_id` values so files can be deduplicated during analysis.

## Collection plan

Use a staged rollout rather than waiting for a single magic sample-size number:

- usability check: roughly 5–10 people is useful for finding obvious flow/copy problems;
- small field/shakedown pilot: roughly 20–50 participants can expose export, completion, missingness, and item-distribution issues;
- psychometric pilot: plan for at least around 100 complete sessions and preferably substantially more before interpreting reliability/item statistics strongly;
- factor-structure work on a 47-item instrument usually needs a larger sample than the early usability/pilot stages. Final target should be justified in the research protocol rather than inferred from one rule of thumb.

Do not treat these numbers as proof of validation. Sample adequacy depends on the analysis, response distributions, item structure, and research design.

## Privacy

The current app does not automatically upload pilot data. Export is manual and participant-controlled. Free-text Creative responses may be present in the CSV, so the receiving location must be appropriate for research data.

## RC1.2 auto-sync note

When `POTENTIA_PILOT_SYNC_ENDPOINT` and `POTENTIA_PILOT_SYNC_TOKEN` are configured in the build, completed Pilot Pseudonim sessions are also queued for automatic HTTPS sync. Manual CSV/JSON export remains available as a backup. See `AUTO_SYNC_SETUP.md`.
