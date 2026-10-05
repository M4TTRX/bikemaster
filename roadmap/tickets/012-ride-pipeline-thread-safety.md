---
id: "012"
title: Thread-safety of ride pipeline mutable state
type: bug
priority: P2
status: todo
area: app
effort: M
audit_ref: [B10]
depends_on: ["004", "005"]
assignee:
---

# 012 — Thread-safety of ride pipeline mutable state

## Problem
Plain `var` fields are read and written from several threads with no synchronisation:
- `DefaultRideRepository`: `totalPowerJoules`, `lastLocation`, `locationJob`/`speedSensorJob`/`timerJob`. Coroutines on `Dispatchers.Default` (multi-threaded) touch them, as do UI-thread calls (`startRide`, `resetRide`).
- `SingleSensorBleManager`: `lastWheelRevs`, `lastCrankRevs`, etc. Nordic BLE callbacks write them, and main-looper timeout runnables reset them.
- `MockRideSimulator`: `tick`, `totalPowerJoules`.

This can cause lost updates, torn reads and occasional wrong deltas.

## Evidence
- [`RideRepository.kt#L57`](../../app/src/main/java/com/velo/app/data/RideRepository.kt), [`#L90-L95`](../../app/src/main/java/com/velo/app/data/RideRepository.kt)
- [`SingleSensorBleManager.kt#L56-L90`](../../app/src/main/java/com/velo/app/sensors/SingleSensorBleManager.kt)

## Proposed fix
- Run all ride-state mutation on one serial context, e.g. `Dispatchers.Default.limitedParallelism(1)`, or an actor/`Channel` feeding the `RideAccumulator` (tickets 004/005).
- In `SingleSensorBleManager`, post the timeout resets onto the same handler/thread the BLE callbacks run on, or protect the fields with a lock.

## Acceptance criteria
- [ ] Every mutable ride-state field has a single, documented owning thread/context
- [ ] A stress unit test (e.g. 10k concurrent samples via `runTest` + multiple dispatchers) gives deterministic totals

## Out of scope
Changing the BLE library.
