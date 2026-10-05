---
id: "009"
title: Harden RideTrackingService
type: bug
priority: P1
status: todo
area: app
effort: M
audit_ref: [B7]
depends_on: []
assignee:
---

# 009 — Harden RideTrackingService

## Problem
- The manifest declares `foregroundServiceType="location|connectedDevice"`, but `startForeground` only passes `FOREGROUND_SERVICE_TYPE_LOCATION`. The BLE connection isn't covered by the FGS type.
- The notification never changes from "Speed: 0.0 km/h", "Ride resumed", etc.
- `START_STICKY` restarts the service with a `null` intent. The `when` has no branch for that, so a zombie service stays alive with no foreground notification.
- `catch (e: Throwable)` around `startForeground` hides real failures (e.g. missing location permission on Android 14+) and leaves the ride running without an FGS. The OS can then kill it.
- `onDestroy` is an empty override.

## Evidence
- [`AndroidManifest.xml#L43-L47`](../../app/src/main/AndroidManifest.xml)
- [`RideTrackingService.kt#L36-L72`](../../app/src/main/java/com/velo/app/service/RideTrackingService.kt)
- [`RideViewModel.kt#L164-L177`](../../app/src/main/java/com/velo/app/viewmodel/RideViewModel.kt): service start errors are swallowed

## Proposed fix
- Pass `LOCATION or CONNECTED_DEVICE` (API 29+/34+ as appropriate), checking permissions first.
- Return `START_NOT_STICKY` (the ride state is in memory and won't survive a restart anyway), or handle `null` by stopping itself.
- Have the service observe `rideRepository.telemetry` (sampled at ~1 Hz, or less often to save battery) and update the notification with speed/distance/time. Add Pause/Resume/Stop notification actions.
- Report FGS start failure back to the UI, e.g. via a repository error state, instead of swallowing it.
- Remove the empty `onDestroy` override, or use it to cancel the observing scope.

## Acceptance criteria
- [ ] On Android 14+, starting a ride with location + BT permissions gives an FGS with both types (`adb shell dumpsys activity services com.velo.app`)
- [ ] The notification shows live speed/distance while recording
- [ ] Killing the process doesn't leave a notification-less service running
- [ ] Missing permission shows a user-visible message rather than failing silently

## Out of scope
Persisting ride state across process death. Possible follow-up ticket.
