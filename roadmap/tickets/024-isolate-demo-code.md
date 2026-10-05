---
id: "024"
title: Isolate emulator/demo code from production paths
type: refactor
priority: P2
status: todo
area: app
effort: M
audit_ref: [H7]
depends_on: []
assignee:
---

# 024 — Isolate emulator/demo code from production paths

## Problem
Emulator mocking and demo device injection logic are deeply intertwined with production BLE hardware code:
1. `BleSensorManager.connectDevice` catches `IllegalArgumentException` (which only happens with invalid MAC strings used in tests/mocks) and fakes active connection flags (`_isSpeedConnected.value = true`).
2. `BleSensorManager.startScanning` launches a 3-second delay coroutine that injects 4 hardcoded fake sensors (`Decathlon Dual SPD/CAD`, `Stages Power L`, etc.) into `_discoveredSensors`.
3. `onScanFailed` suppresses scan error callbacks if running on emulator.

This contaminates production BLE logic, introduces unexpected side effects on real hardware, and makes real Bluetooth bugs harder to detect.

## Evidence
- [`BleSensorManager.kt#L283-L317`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): Simulated demo devices injection.
- [`BleSensorManager.kt#L373-L382`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): Catches MAC parse exception and synthesizes connection state.

## Proposed fix
1. Separate real BLE implementation from mock/demo sensor management:
   - Define a `BleScanner` interface with `ProductionBleScanner` and `MockBleScanner` implementations.
   - Or encapsulate mock device injection behind an explicit `MockSensorInjector` that only operates when `isMockEnabled` is explicitly requested by settings/build variant.
2. In `connectDevice`, reject invalid MAC addresses cleanly with an error or validation check instead of silently pretending the hardware connected.

## Acceptance criteria
- [ ] `ProductionBleScanner` / real scan path contains zero hardcoded device names or addresses.
- [ ] Invalid MAC addresses fail gracefully without flipping `_is*Connected` flags to true.
- [ ] Mock simulator / emulator sensor testing continues working when explicitly enabled.

## Out of scope
Flavor-based source set separation (`demo` vs `full`).
