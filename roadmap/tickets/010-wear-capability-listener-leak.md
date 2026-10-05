---
id: "010"
title: Capability listener leak and stale isConnected
type: bug
priority: P2
status: todo
area: app
effort: S
audit_ref: [B8]
depends_on: []
assignee:
---

# 010 — Capability listener leak and stale isConnected

## Problem
- `WearOsManager.startListening()` registers a **new** capability listener lambda every call. It's called from `init`, `startRide`, `resumeRide` and `resetRide`, so listeners pile up and are never removed.
- `_isConnected` is set to `true` by node discovery or incoming messages, but never back to `false`. The UI keeps showing the watch as connected after it disconnects.
- `isListening` isn't thread-safe.

## Evidence
- [`WearOsManager.kt#L45-L81`](../../app/src/main/java/com/velo/app/sensors/WearOsManager.kt)
- [`RideRepository.kt#L136`](../../app/src/main/java/com/velo/app/data/RideRepository.kt), [`#L337`](../../app/src/main/java/com/velo/app/data/RideRepository.kt), [`#L359`](../../app/src/main/java/com/velo/app/data/RideRepository.kt), [`#L383`](../../app/src/main/java/com/velo/app/data/RideRepository.kt)

## Proposed fix
- Store the capability listener in a field. Register it once and remove it in `stopListening()`.
- Separate "register listeners" (once) from "refresh state" (callable often).
- Derive `isConnected` from `CapabilityInfo.nodes` / `NodeClient.connectedNodes`. Also add a heartbeat timeout: if no HR message arrives for N seconds, mark the watch disconnected and reset `wearOsHeartRate` to 0.

## Acceptance criteria
- [ ] Calling start/resume/reset 10× leaves exactly one capability listener registered
- [ ] Turning the watch's Bluetooth off flips `isWearOsConnected` to false within the timeout, and HR falls back to 0 / BLE HR

## Out of scope
Protocol changes (ticket 030).
