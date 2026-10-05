---
id: "019"
title: Validate Wear Data Layer payloads
type: security
priority: P2
status: todo
area: app, wear
effort: S
audit_ref: [S6]
depends_on: []
assignee:
---

# 019 — Validate Wear Data Layer payloads

## Problem
Incoming messages from the Wearable Data Layer are accepted with minimal validation:
- Phone parses heart rate directly as a 4-byte Int without bounds checking: negative values or absurd numbers (e.g. 5000 bpm) are accepted and forwarded to telemetry.
- Watch accepts any arbitrary status string and falls back to `CONNECTED` unless explicitly `PAUSED` or `NOT_CONNECTED`, masking error states.

## Evidence
- [`WearOsManager.kt#L147-L149`](../../app/src/main/java/com/velo/app/sensors/WearOsManager.kt): Unvalidated `ByteBuffer.wrap(data).int`
- [`WatchRideStatus.kt#L13-L19`](../../wear/src/main/java/com/velo/wear/data/WatchRideStatus.kt): `else -> CONNECTED` fallback

## Proposed fix
1. On phone (`WearOsManager`):
   - Validate payload size is exactly 4 bytes.
   - Enforce plausible heart rate range (e.g. `25..250 bpm`). Discard or clamp out-of-range values.
2. On watch (`WatchRideStatus`):
   - Replace open string parsing with strict enum matching (`RideStatus.entries.find { ... }`).
   - If an unrecognized string is received, log a warning and retain previous state rather than assuming `CONNECTED`.

## Acceptance criteria
- [ ] Negative or >250 BPM payloads are rejected with a warning.
- [ ] Unknown status string does not trigger unexpected UI state transitions.
- [ ] Unit tests for payload validation on both phone and watch.

## Out of scope
Payload encryption (Wearable Data Layer is already encrypted over Bluetooth/Wi-Fi).
