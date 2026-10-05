---
id: "022"
title: Stop swallowing exceptions silently
type: refactor
priority: P2
status: todo
area: app, wear
effort: M
audit_ref: [H5]
depends_on: []
assignee:
---

# 022 — Stop swallowing exceptions silently

## Problem
There are over 20 empty `catch (e: Exception)` and `catch (e: Throwable)` blocks across `app` and `wear`. Critical failures (such as foreground service start rejections, BLE GATT close issues, Wearable interaction errors, and DataStore parsing corruptions) fail completely silently, making bugs nearly impossible to diagnose in logs or bug reports.

## Evidence
- [`RideTrackingService.kt#L48-L54`](../../app/src/main/java/com/velo/app/service/RideTrackingService.kt): Catches `Throwable` around `startForeground` and calls `stopSelf()` with zero logging.
- [`RideViewModel.kt#L174-L176`](../../app/src/main/java/com/velo/app/viewmodel/RideViewModel.kt): Empty catch around starting service.
- [`BleSensorManager.kt#L328-L330`](../../app/src/main/java/com/velo/app/sensors/BleSensorManager.kt): Empty catch around stopping scan.
- [`SingleSensorBleManager.kt#L163-L170`](../../app/src/main/java/com/velo/app/sensors/SingleSensorBleManager.kt): Empty catch around disconnect & close.

## Proposed fix
1. Audit all 20+ `catch` blocks.
2. Narrow exceptions: catch specific checked types (`SecurityException`, `IllegalArgumentException`, `IOException`) instead of catch-all `Throwable`.
3. Log caught exceptions with appropriate severity (`Log.w` or `Log.e`) including stack traces in debug builds.
4. Expose operational failures (e.g. FGS permission denied, BLE adapter powered off) to ViewModel state so the UI can inform the user.

## Acceptance criteria
- [ ] 0 empty catch blocks in production code.
- [ ] No generic `catch (Throwable)` unless immediately rethrown or shutting down process.
- [ ] Service start failures log meaningful diagnostic messages.

## Out of scope
Crashlytics integration.
