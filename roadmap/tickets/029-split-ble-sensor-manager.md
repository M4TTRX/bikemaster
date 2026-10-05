---
id: "029"
title: Extract SensorClassifier from BleSensorManager
type: refactor
priority: P2
status: todo
area: app
effort: M
audit_ref: [C3]
depends_on: []
assignee:
---

# 029 — Extract SensorClassifier from BleSensorManager

## Problem
`BleSensorManager.kt` contains 446 lines mixing multiple disparate responsibilities:
1. Bluetooth scanning and callback registration.
2. Keyword matching and heuristic peripheral classification (filtering TVs, headphones vs recognizing Decathlon, Stages, Wahoo, HRM).
3. Nordic connection manager lifecycle for multiple devices.
4. Aggregating active connection flags and telemetry StateFlows.
5. Emulator mock injection (see Ticket 024).

The heuristic device classification logic is pure domain logic, but is currently tangled inside scan callbacks, making it difficult to unit test.

## Evidence
- [`BleSensorManager.kt#L74-L101`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): Keyword lists.
- [`BleSensorManager.kt#L188-L239`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): 50-line heuristic classification inside scan callback.

## Proposed fix
1. Extract a pure class `SensorClassifier`:
   ```kotlin
   class SensorClassifier {
       fun classify(name: String?, serviceUuids: List<UUID>): ClassificationResult
   }
   ```
2. Move peripheral brand lists (`CYCLING_BRAND_WHITELIST`, `EXCLUDED_DEVICE_KEYWORDS`) into `SensorClassifier`.
3. Add unit tests for `SensorClassifier` verifying that Decathlon, Garmin, Polar, Wahoo, Stages, and various smart TVs / AirPods are correctly classified or filtered without needing an Android Context or Bluetooth hardware.

## Acceptance criteria
- [ ] `BleSensorManager` delegates classification to `SensorClassifier`.
- [ ] 100% unit test coverage for device name and service UUID classification.

## Out of scope
Custom user sensor naming.
