---
id: "004"
title: Fix wheel-sensor distance under/double counting
type: bug
priority: P0
status: todo
area: app
effort: M
audit_ref: [B1]
depends_on: []
assignee:
---

# 004 — Fix wheel-sensor distance under/double counting

## Problem
Ride distance from a BLE speed sensor is wrong in both directions:
1. **Under-count:** `distanceDeltaFlow` is a `StateFlow`. `StateFlow` drops values equal to the previous one, so at constant speed (identical deltas per notification) most increments are lost.
2. **Double-count:** the repository `combine`s `speedFlow` with `distanceDeltaFlow`. Whenever speed changes, `combine` re-emits with the *previous* delta, which is added again.

Distance also feeds average speed, so both metrics are affected.

## Evidence
- [`BleSensorManager.kt#L44-L45`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): `MutableStateFlow(0.0)` for deltas
- [`BleSensorManager.kt#L393-L398`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): speed and delta are pushed together
- [`RideRepository.kt#L264-L282`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `combine(speedFlow, distanceDeltaFlow)` adds `deltaDist` on each emission

## Proposed fix
- Model wheel data as **events**: e.g. `MutableSharedFlow<WheelSample>(extraBufferCapacity = 64)` where `WheelSample(speedKmh, distanceDeltaM, timestamp)`, emitted once per CSC notification.
- Introduce a pure, unit-testable `RideAccumulator` (in `data/` or `domain/`) that consumes samples and owns distance / max speed / avg speed. Ticket 005 will reuse it.
- Keep `speedFlow` as a `StateFlow` for the live display.

## Acceptance criteria
- [ ] 100 identical wheel samples of 2.1 m → distance increases by exactly 210 m
- [ ] A speed-only change does not add distance
- [ ] Samples arriving while not `RECORDING` are not counted
- [ ] `RideAccumulator` has unit tests covering the above (see ticket 025)

## Out of scope
GPS distance filtering (unchanged), elevation (ticket 008).
