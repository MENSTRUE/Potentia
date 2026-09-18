# Potentia Growth V3 — Guided Practice & Reflection

Growth V3 replaces the old self-report-only `Tandai Selesai` flow.

## Product principle

The Growth tab is not a proof that a user's ability improved. It is a guided
self-reflection and micro-practice layer built on top of the latest pilot result.

Two activity modes are used:

1. **Mini Challenge**
   - The app can verify a concrete answer.
   - A wrong answer gives a retry hint.
   - The activity is recorded only after a correct interaction.
   - The result is still practice, not a new psychometric measurement.

2. **Refleksi Terpandu**
   - The user must fill required reflection prompts before saving.
   - The app records that the reflection was completed.
   - The app does not claim the content is honest, correct, or evidence of ability gain.

## Persistence

`GrowthStorage` now uses `growth_progress_v3` and stores progress by
`weekKey|dimensionId`, so multiple weekly/dimension records do not overwrite one another.

Growth V2 data under `growth_progress_v2` can still be read as a compatibility path.
The old value is not silently deleted.

## Wording

The Growth screen no longer emphasizes the numerical 0–100 score. It explains that
the focus area is selected from the pattern of the latest assessment and is offered
for exploration, not as a diagnosis or deficit label.

Completion language is now:
- `Challenge terverifikasi`
- `Refleksi tersimpan`
- `aktivitas dijalani`

not `kemampuan meningkat`.

## Current catalog

18 activities:
- Logical: 3
- Creative: 3
- Verbal: 3
- Spatial: 3
- Social: 3
- Practical: 3

5 are system-checkable Mini Challenges and 13 are Guided Reflections.
