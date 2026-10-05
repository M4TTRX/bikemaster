---
id: "020"
title: Use BLE ScanFilters for sensor discovery
type: perf
priority: P2
status: todo
area: app
effort: S
audit_ref: [S7]
depends_on: []
assignee:
---

# 020 — Use BLE ScanFilters for sensor discovery

## Problem
`BleSensorManager.startScanning()` calls `scanner.startScan(null, settings, callback)` with a null filter list at `SCAN_MODE_LOW_LATENCY`. This consumes excessive battery and forces the phone to process advertising packets from every light bulb, car, TV, and beacon in the vicinity, relying on heavy post-filtering in Kotlin.

## Evidence
- [`BleSensorManager.kt#L268`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): `scanner.startScan(null, settings, callback)`

## Proposed fix
1. Build a list of `ScanFilter`s targeting standard Bluetooth SIG cycling GATT service UUIDs:
   - CSC Service (`00001816-0000-1000-8000-00805f9b34fb`)
   - HRM Service (`0000180D-0000-1000-8000-00805f9b34fb`)
   - Cycling Power Service (`00001818-0000-1000-8000-00805f9b34fb`)
2. For legacy sensors that omit service UUIDs from the main advertising packet, provide a "Scan All Peripherals" fallback toggle or run an unfiltered scan only after a filtered pass yields no results.
3. Lower default scan mode to `SCAN_MODE_BALANCED` after the first 5 seconds to preserve battery.

## Acceptance criteria
- [ ] Primary scan passes `List<ScanFilter>` to `BluetoothLeScanner.startScan`.
- [ ] Standard cycling sensors (CSC, HRM, Power) are immediately discovered.
- [ ] CPU wakeups during scanning in RF-heavy environments are reduced.

## Out of scope
Background continuous BLE scanning.
