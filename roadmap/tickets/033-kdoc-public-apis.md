---
id: "033"
title: KDoc for public APIs and GATT parsing
type: docs
priority: P2
status: todo
area: app, wear
effort: S
audit_ref: [C7]
depends_on: []
assignee:
---

# 033 — KDoc for public APIs and GATT parsing

## Problem
Across 40+ production Kotlin files, there are only 8 KDoc blocks. Critical low-level operations—specifically Bluetooth GATT packet unpacking, bitmask flags, and rollover arithmetic—contain minimal commentary. Without references to official Bluetooth SIG specifications, future contributors or AI agents cannot easily determine whether bit shifting operations are correct.

## Evidence
- Only 8 `/**` blocks found across `app/src/main` and `wear/src/main`.
- [`SingleSensorBleManager.kt#L221-L379`](../../app/src/main/java/com/velo/app/sensors/SingleSensorBleManager.kt): Complex bitwise unpacking with brief inline comments.

## Proposed fix
1. Add comprehensive KDoc to public interfaces:
   - `RideRepository`
   - `BleSensorManager`
   - `WearOsManager`
   - `WearCopilotManager`
2. Document GATT characteristic parsing methods with references to Bluetooth SIG specifications:
   - CSC Measurement (0x2A5B): Wheel revolution data, crank revolution data, flags bit 0 & 1.
   - Heart Rate Measurement (0x2A37): 8-bit vs 16-bit format flag.
   - Cycling Power Measurement (0x2A63): Instantaneous power and crank revolution data flag (bit 5).

## Acceptance criteria
- [ ] All public repositories, managers, and GATT parser routines documented with KDoc.
- [ ] Relevant Bluetooth SIG specification numbers cited for GATT parsers.

## Out of scope
Private UI composable KDoc.
