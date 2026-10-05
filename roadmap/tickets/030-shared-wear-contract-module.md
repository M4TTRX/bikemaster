---
id: "030"
title: Shared wear contract module
type: refactor
priority: P2
status: todo
area: app, wear, build
effort: M
audit_ref: [C4]
depends_on: []
assignee:
---

# 030 — Shared wear contract module

## Problem
The communication contract between `:app` and `:wear` is duplicated across both modules using loose string literals:
- Path constants (`/velo/heart_rate`, `/velo/status`, `/velo/open_companion`, `/velo/status_request`) are declared independently in both `WearOsManager.kt` and `WearCopilotManager.kt`.
- Status values are converted to and from arbitrary UTF-8 strings (`RECORDING`, `PAUSED`, `IDLE`, `FINISHED`) with no shared compile-time type safety.
- If one module modifies a path or adds an enum entry, the other module will silently break at runtime.

## Evidence
- [`WearOsManager.kt#L21-L26`](../../app/src/main/java/com/velo/app/sensors/WearOsManager.kt)
- [`WearCopilotManager.kt#L29-L32`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt)

## Proposed fix
1. Introduce a lightweight `:core:common` or `:shared` Kotlin JVM library module included in both `:app` and `:wear`.
2. Define the shared protocol contract:
   - Shared path constants.
   - Shared `RideStatus` enum.
   - Binary serialization / deserialization helpers for heart rate and status messages.
3. Replace local constant declarations in both modules with references to the shared module.

## Acceptance criteria
- [ ] Protocol paths and message types defined in a single location.
- [ ] Changing a path or message type produces a compile-time error in both modules if not updated.

## Out of scope
Sharing Compose UI components between phone and watch.
