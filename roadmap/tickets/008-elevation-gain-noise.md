---
id: "008"
title: Elevation gain inflated by GPS altitude noise
type: bug
priority: P1
status: todo
area: app
effort: M
audit_ref: [B6]
depends_on: ["004"]
assignee:
---

# 008 — Elevation gain inflated by GPS altitude noise

## Problem
Every positive altitude delta above 0.5 m between consecutive GPS fixes is added to elevation gain. GPS vertical error is typically ±5–15 m, so jitter on flat roads adds up to tens or hundreds of metres of gain that didn't happen.

## Evidence
- [`RideRepository.kt#L299-L302`](../../app/src/main/java/com/velo/app/data/RideRepository.kt)

## Proposed fix
In `RideAccumulator` (ticket 004):
- Smooth altitude (e.g. exponential moving average or a 5-sample median).
- Apply hysteresis: only count a climb once smoothed altitude rises ≥ 3–5 m above the last low point.
- Prefer barometric altitude (`Sensor.TYPE_PRESSURE` → `SensorManager.getAltitude`) when available, falling back to GPS. The barometer can be a follow-up ticket.
- Use `Location.verticalAccuracyMeters` (API 26+) to discard poor fixes.

## Acceptance criteria
- [ ] Unit test: a flat route with ±8 m random altitude noise over 30 min gives < 10 m gain
- [ ] Unit test: a steady 100 m climb gives 95–105 m gain

## Out of scope
Elevation loss, grade %.
