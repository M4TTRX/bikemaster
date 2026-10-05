---
id: "037"
title: Phone data-pipeline architecture doc
type: docs
priority: P2
status: todo
area: docs
effort: S
audit_ref: [D4]
depends_on: ["004", "005"]
assignee:
---

# 037 — Phone data-pipeline architecture doc

## Problem
The handheld telemetry and sensor pipeline in `:app` combines multiple asynchronous inputs (GPS FusedLocation, BLE speed/cadence, BLE power, BLE heart rate, Wear OS wrist heart rate, Mock simulator, User settings) through nested Kotlin Coroutine `combine` flows. Without an architectural document, it is difficult to understand sensor precedence rules, downsampling mechanisms, or calculation workflows.

## Evidence
- [`RideRepository.kt#L100-L134`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): Complex nested `combine` flows.

## Proposed fix
Create `docs/architecture-data-pipeline.md`:
1. **Flow Diagram**: Visualizing how data moves from hardware sensors -> managers -> `RideRepository` -> `RideAccumulator` -> `RideViewModel` -> UI.
2. **Speed Source Arbitration**: Explaining `AUTO` vs `SPEED_SENSOR` vs `GPS` priorities.
3. **Heart Rate Fallback**: Documenting BLE chest strap priority over Wear OS optical sensor.
4. **Lifecycle State Machine**: Depicting transitions between `IDLE`, `RECORDING`, `PAUSED`, and `FINISHED`.

## Acceptance criteria
- [ ] `docs/architecture-data-pipeline.md` created with clear Mermaid state and pipeline diagrams.
- [ ] Sensor precedence and fallback rules explicitly documented.

## Out of scope
Backend API documentation.
