---
id: "023"
title: Remove duplicated code and magic numbers
type: refactor
priority: P2
status: todo
area: app, wear
effort: S
audit_ref: [H6, H10]
depends_on: []
assignee:
---

# 023 — Remove duplicated code and magic numbers

## Problem
Several pieces of logic and magic literals are duplicated throughout the codebase:
- `RideTelemetry(...)` full 17-parameter constructor is typed out in 4 separate locations even though all parameters have defaults.
- Default wheel circumference `2105` is hardcoded across 5 different files.
- `startRide()` and `resumeRide()` in `RideRepository` are identical line-for-line.
- Tile source parsing (`slot=source;...`) is duplicated in 3 methods in `SettingsRepository`.
- `fontCache` logic is duplicated between `app/ui/theme/Theme.kt` and `wear/theme/Type.kt`.
- `RideViewModel` instantiates a redundant duplicate `SettingsRepository(application)` rather than using `VeloApplication.settingsRepository`.

## Evidence
- [`RideRepository.kt#L68-L86`](../../app/src/main/java/com/velo/app/data/RideRepository.kt), [`#L386-L404`](../../app/src/main/java/com/velo/app/data/RideRepository.kt)
- [`MockRideSimulator.kt#L23-L39`](../../app/src/main/java/com/velo/app/sensors/MockRideSimulator.kt), [`#L105-L121`](../../app/src/main/java/com/velo/app/sensors/MockRideSimulator.kt)
- [`SettingsRepository.kt#L102`](../../app/src/main/java/com/velo/app/data/SettingsRepository.kt), [`SettingsScreen.kt#L74`](../../app/src/main/java/com/velo/app/ui/settings/SettingsScreen.kt), [`SingleSensorBleManager.kt#L22`](../../app/src/main/java/com/velo/app/sensors/SingleSensorBleManager.kt)

## Proposed fix
1. Use `RideTelemetry()` default constructor or a single `RideTelemetry.initial()` companion factory.
2. Define `const val DEFAULT_WHEEL_CIRCUMFERENCE_MM = 2105` in a central model/constants file.
3. Consolidate `startRide` and `resumeRide` into a shared internal method.
4. Extract helper functions `parseTileSources(String)` and `formatTileSources(Map)` in `SettingsRepository`.
5. Have `RideViewModel` inject or read `VeloApplication.settingsRepository`.

## Acceptance criteria
- [ ] Central constant for default wheel circumference.
- [ ] No duplicated `RideTelemetry` zero-initialization boilerplate.
- [ ] `RideViewModel` uses the single application-scoped `SettingsRepository` instance.

## Out of scope
Full dependency injection framework (Hilt/Koin).
