---
id: "013"
title: Watch permission prompt loop
type: bug
priority: P2
status: todo
area: wear
effort: S
audit_ref: [B11]
depends_on: []
assignee:
---

# 013 — Watch permission prompt loop

## Problem
`checkAndRequestPermissions()` runs in both `onCreate` and **every** `onResume`. Dismissing the system permission dialog resumes the activity, which immediately asks again. After a "deny once" the user can get stuck in a prompt loop. There's no explanation of why heart-rate access is needed.

## Evidence
- [`wear/MainActivity.kt#L41`](../../wear/src/main/java/com/velo/wear/MainActivity.kt), [`#L51-L62`](../../wear/src/main/java/com/velo/wear/MainActivity.kt), [`#L69-L73`](../../wear/src/main/java/com/velo/wear/MainActivity.kt)

## Proposed fix
- Request once per launch, tracked with a `rememberSaveable`/instance flag.
- Use `shouldShowRequestPermissionRationale` to show a short in-app explanation screen. On permanent denial, show a "Open settings" chip instead of prompting.
- Keep `registerHeartRateSensor()` in the permission result callback.

## Acceptance criteria
- [ ] Denying once doesn't show the dialog again until the user taps an explicit "Grant" action
- [ ] Granting starts HR streaming without restarting the app

## Out of scope
Health Services migration.
