---
id: "005"
title: Fix average power calculation
type: bug
priority: P0
status: todo
area: app
effort: S
audit_ref: [B2]
depends_on: ["004"]
assignee:
---

# 005 — Fix average power calculation

## Problem
`totalPowerJoules += watts` runs once per `powerFlow` **emission**, then gets divided by elapsed seconds. Power meters notify at 1–4 Hz depending on the model, and `StateFlow` drops repeated equal values. So average power is inflated on fast meters and deflated at steady effort. The variable name also wrongly suggests joules (see ticket 031).

## Evidence
- [`RideRepository.kt#L150-L168`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): accumulate per emission
- [`RideRepository.kt#L253`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `avgPower = totalPowerJoules / elapsed`

## Proposed fix
In the `RideAccumulator` from ticket 004, integrate power over time. On each 1 Hz timer tick while `RECORDING`, add `currentPowerWatts × 1 s` to `energyJoules`. Then `avgPower = energyJoules / elapsedSeconds`. Optionally decide whether zeros (coasting) count towards the average (the industry default is yes) and document it.

## Acceptance criteria
- [ ] Constant 200 W for 60 ticks → avg 200 W, whatever the notification rate (1, 2 or 4 Hz) and whether values repeat
- [ ] Paused time does not count
- [ ] Unit tests added

## Out of scope
Normalized power, power zones.
