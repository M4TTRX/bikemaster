---
id: "031"
title: Fix misleading names and non-null settings dependency
type: refactor
priority: P2
status: todo
area: app
effort: S
audit_ref: [C5]
depends_on: []
assignee:
---

# 031 — Fix misleading names and non-null settings dependency

## Problem
Several identifiers and method contracts are misleading:
1. `totalPowerJoules`: Does not measure joules. It sums raw watts per incoming BLE emission (which arrive at variable frequencies).
2. `isHeartRateConnected`: Evaluates to true whenever `hr > 0`, even if the heart rate sensor just disconnected and the last reading was held.
3. In `DefaultRideRepository`: `settingsRepository` is typed as nullable `SettingsRepository? = null`. As a result, the codebase is littered with `if (settingsRepository != null)` checks, but in production the app always passes a non-null instance.

## Evidence
- [`RideRepository.kt#L90`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `private var totalPowerJoules: Long = 0L`
- [`RideRepository.kt#L120`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `bleHr || wearConn || hr > 0`
- [`RideRepository.kt#L54`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `private val settingsRepository: SettingsRepository? = null`

## Proposed fix
1. When energy calculation is fixed in Ticket 005, name the accumulator accurately (`accumulatedEnergyJoules`).
2. Make `isHeartRateConnected` strictly represent active BLE/Wear connection state rather than testing whether the last reading was non-zero.
3. Make `settingsRepository` a required, non-null dependency in `DefaultRideRepository`.

## Acceptance criteria
- [ ] No misleading metric variable names.
- [ ] Zero unnecessary `if (settingsRepository != null)` checks in `RideRepository`.

## Out of scope
Other repository redesigns.
