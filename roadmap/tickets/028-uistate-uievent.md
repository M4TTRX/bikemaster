---
id: "028"
title: Introduce UiState and UiEvent in presentation layer
type: refactor
priority: P2
status: todo
area: app
effort: M
audit_ref: [C2]
depends_on: []
assignee:
---

# 028 — Introduce UiState and UiEvent in presentation layer

## Problem
Currently, 37 separate state values and callbacks are drilled down through `MainActivity` -> `MainScreen` -> `SettingsScreen` and `RideDashboardScreen`. In `MainActivity`, 14 individual `collectAsStateWithLifecycle()` calls are invoked manually in the Compose content block. Adding a new setting or telemetry property requires editing method signatures across 4 files.

## Evidence
- [`MainActivity.kt#L50-L71`](../../app/src/main/java/com/velo/app/MainActivity.kt): 14 StateFlow collections.
- [`MainScreen.kt#L28-L63`](../../app/src/main/java/com/velo/app/ui/MainScreen.kt): 37 constructor arguments.

## Proposed fix
1. Consolidate dashboard and settings states into cohesive immutable state models:
   ```kotlin
   data class DashboardUiState(...)
   data class SettingsUiState(...)
   ```
2. Model user actions as a sealed interface hierarchy:
   ```kotlin
   sealed interface RideUiEvent {
       data object Start : RideUiEvent
       data object Pause : RideUiEvent
       data class SelectTile(val slot: Int, val metric: MetricType, val source: String?) : RideUiEvent
       ...
   }
   ```
3. Expose `val uiState: StateFlow<MainUiState>` and `fun onEvent(event: RideUiEvent)` from `RideViewModel`.

## Acceptance criteria
- [ ] `MainScreen` parameter list reduced from 37 arguments to `(uiState: MainUiState, onEvent: (RideUiEvent) -> Unit)`.
- [ ] `MainActivity` collects a single unified state object with lifecycle awareness.

## Out of scope
MVI architectural overhaul of the repository layer.
