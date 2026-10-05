---
id: "025"
title: Unit tests for parsing, persistence and accumulation
type: test
priority: P1
status: todo
area: app, wear
effort: M
audit_ref: [H8, A2]
depends_on: ["004", "005"]
assignee:
---

# 025 — Unit tests for parsing, persistence and accumulation

## Problem
The existing test suite (`BlePairingTest.kt`, `RideStateTest.kt`, `RideTelemetryTest.kt`, `WatchRideStatusTest.kt`) primarily asserts enum entries counts and trivial data-class properties. There is **zero test coverage** for the code paths that have the highest mathematical and hardware failure risk:
- Bluetooth GATT byte parsing (CSC speed/cadence unpacking, 16-bit crank rollover, 32-bit wheel rollover, HRM 8-bit vs 16-bit flags, Power watts/crank offset parsing).
- `RideAccumulator` distance, average speed, and average power calculations (B1 and B2 bugs would have been caught).
- Settings storage serialization and deserialization.

## Evidence
- [`app/src/test/java/com/velo/app/model/BlePairingTest.kt`](../../app/src/test/java/com/velo/app/model/BlePairingTest.kt): Asserts enum sizes and titles.
- Zero tests in `app/src/test` covering `SingleSensorBleManager` byte packet handlers.

## Proposed fix
1. Create `CscMeasurementParserTest`:
   - Test wheel revolution rollover across 32-bit boundary (`0xFFFFFFFF` -> `0x00000005`).
   - Test crank revolution rollover across 16-bit boundary (`0xFFFF` -> `0x000A`).
   - Test 1024Hz fractional time rollover.
2. Create `HrmMeasurementParserTest`:
   - Test 8-bit format (flag bit 0 = 0).
   - Test 16-bit format (flag bit 0 = 1).
3. Create `CyclingPowerMeasurementParserTest`:
   - Test instantaneous power (W) unpacking.
   - Test crank revolution parsing when bit 5 is enabled with variable optional offsets.
4. Create `RideAccumulatorTest` (testing the accumulator introduced in Tickets 004/005).

## Acceptance criteria
- [ ] Comprehensive unit tests for all GATT parser edge cases.
- [ ] Rollover arithmetic validated for 16-bit and 32-bit counters.
- [ ] Tests run fast and completely offline with standard JUnit 4/5.

## Out of scope
Robolectric / instrumented on-device tests.
