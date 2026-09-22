# POTENTIA — Live Pilot Monitor Guide

After RC1.3 Apps Script is deployed, use the generated Google Sheet as the main collection monitor.

## Dashboard

The `Dashboard` sheet shows:

- synced session count;
- unique pseudonymous participant count;
- complete sessions with at least 47 response rows;
- sessions that need review because they contain fewer than 47 response rows;
- total raw response rows;
- latest received timestamp;
- app, assessment, and scoring versions seen in uploaded data;
- the latest 20 synced sessions.

This dashboard is for collection operations, not psychometric interpretation.

## Sessions

One row equals one accepted pilot session. Use `session_id` as the unique upload key. Duplicate retries from the same session are ignored by the endpoint.

## Responses

One row equals one item response. A normal complete assessment is expected to contribute about 47 rows.

Free-text responses can appear in this sheet. Restrict access to the research team and do not publish the raw sheet publicly.

## CSV backups

The configured Drive folder contains:

- `potentia_pilot_sessions_live.csv`
- `potentia_pilot_responses_live.csv`

These are refreshed after a newly accepted session. They are convenient for Kaggle/Jupyter analysis, but the Google Sheet remains the easier live monitor.

## RC1.4 QA tabs
Debug smoke tests are routed to `QA_Dashboard`, `QA_Sessions`, and `QA_Responses`. These are synthetic technical-test rows and must never be included in pilot analysis or participant counts.
