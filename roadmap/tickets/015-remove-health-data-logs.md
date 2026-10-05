---
id: "015"
title: Remove health data from logs
type: security
priority: P1
status: todo
area: app, wear
effort: S
audit_ref: [S2]
depends_on: []
assignee:
---

# 015 — Remove health data from logs

## Problem
Live Heart Rate (bpm) values, node IDs, and device names are printed to Android logcat at every sample (~1Hz) on both phone and watch. Logcat can be read by any application holding `READ_LOGS` on older OS versions or via system dump reports. PII / health metrics should never leak to logcat in release builds.

## Evidence
- [`WearOsManager.kt#L149`](../../app/src/main/java/com/velo/app/sensors/WearOsManager.kt): `Log.d(TAG, "Received HR from watch: $hr bpm")`
- [`WearCopilotManager.kt#L113`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt): `Log.d(TAG, "SensorManager HR event: $bpm bpm...")`
- [`WearCopilotManager.kt#L125`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt): `Log.d(TAG, "Broadcasting HR $bpm to ${nodes.size} nodes")`

## Proposed fix
1. Remove all log statements printing raw heart rate bpm.
2. Guard remaining debug log calls behind `if (BuildConfig.DEBUG)` or replace with a minimal logging utility.
3. Configure R8 in release builds (see Ticket 017) with `-assumenosideeffects` to strip `android.util.Log.v` and `Log.d`.

## Acceptance criteria
- [ ] `grep -rnE 'Log\.[dewiv]\(.*bpm' app wear` returns 0 occurrences.
- [ ] In release builds, logcat outputs 0 heart rate telemetry entries.

## Out of scope
Analytics / remote telemetry logging.
