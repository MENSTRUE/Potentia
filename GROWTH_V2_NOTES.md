# POTENTIA Growth V2

Growth V2 replaces the previous placeholder `Coba -> Selesai` toggle with a real guided-practice flow.

## User flow

1. Latest assessment chooses the lowest available dimension as the weekly exploration focus.
2. The user selects one of three exercises for that dimension.
3. The challenge card shows the selected exercise.
4. `Mulai Latihan` opens concrete step-by-step instructions.
5. `Tandai Selesai` records completion locally.
6. Weekly completion progress survives app restart.

## Persistence

`GrowthStorage` stores only practice state:
- ISO week key
- focus dimension
- selected exercise ID
- active exercise ID
- completed exercise IDs
- updated timestamp

No raw assessment responses are added to Growth storage.

## Catalog

There are 18 exercises total: 3 each for Logical, Creative, Verbal, Spatial, Social, and Practical.

## Settings integration

`Rekomendasi pengembangan` now has a real effect. If disabled, the Growth tab hides recommendation content and explains how to enable it again.

The weekly reminder toggle is intentionally unchanged and remains a separate future task.
