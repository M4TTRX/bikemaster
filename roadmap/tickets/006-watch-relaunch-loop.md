---
id: "006"
title: Stop watch relaunching every 3 s while recording
type: bug
priority: P0
status: todo
area: wear
effort: S
audit_ref: [B3]
depends_on: []
assignee:
---

# 006 — Stop watch relaunching every 3 s while recording

## Problem
While a ride is recording, the watch app and a high-priority full-screen notification are re-triggered every ~3 s:
1. `WearCopilotManager` polls the phone with `/velo/status_request` every 3 s.
2. The phone replies with `/velo/status` = `RECORDING`.
3. The manifest-registered `WearOsDataListenerService` also receives `/velo/status` and calls `launchCompanionApp()` on **every** `RECORDING` message. That posts a new full-screen notification each time.
4. The activity only cancels the notification in `onCreate`. With `FLAG_ACTIVITY_SINGLE_TOP`, `onCreate` isn't called again, so the notification keeps re-alerting.

Result: battery drain and alert spam on the wrist.

## Evidence
- [`WearCopilotManager.kt#L55-L60`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt), [`#L82-L95`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt)
- [`WearOsDataListenerService.kt#L31-L35`](../../wear/src/main/java/com/velo/wear/communication/WearOsDataListenerService.kt)
- [`WearOsManager.kt#L152-L158`](../../app/src/main/java/com/velo/app/sensors/WearOsManager.kt): phone replies to every request

## Proposed fix
- Only auto-launch on `/velo/open_companion` (an explicit one-shot command). Treat `/velo/status` purely as state, not as a launch trigger.
- Optionally, in the service, skip launching if the activity is already in the foreground.
- Cancel the launch notification in `onNewIntent`/`onResume` as well as `onCreate`.
- Consider pushing status changes only, instead of 3 s polling (see ticket 007).

## Acceptance criteria
- [ ] Starting a ride on the phone opens the watch app **once**
- [ ] No more notifications are posted while recording (verify with `adb shell dumpsys notification` on the watch over ≥60 s)
- [ ] Pause/resume still updates the status dot

## Out of scope
Replacing full-screen intents altogether (see ticket 016).
