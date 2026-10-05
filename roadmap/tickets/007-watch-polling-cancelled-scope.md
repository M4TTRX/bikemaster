---
id: "007"
title: Watch status polling dies after first onStop
type: bug
priority: P0
status: todo
area: wear
effort: S
audit_ref: [B4]
depends_on: []
assignee:
---

# 007 — Watch status polling dies after first onStop

## Problem
`WearCopilotManager.stop()` calls `scope.cancel()` on a `val` scope. When the activity comes back (`onStart` → `start()`), the polling coroutine is launched into the cancelled scope and never runs. After the screen first turns off, the watch never refreshes the connection state or requests ride status.

## Evidence
- [`WearCopilotManager.kt#L39`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt): `private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`
- [`WearCopilotManager.kt#L72-L80`](../../wear/src/main/java/com/velo/wear/communication/WearCopilotManager.kt): `scope.cancel()`
- [`wear/MainActivity.kt#L64-L78`](../../wear/src/main/java/com/velo/wear/MainActivity.kt): `start()`/`stop()` tied to `onStart`/`onStop`

## Proposed fix
Keep a `pollingJob: Job?` and cancel only that in `stop()`. Better still, use `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }` from the activity so the lifecycle owns cancellation.

## Acceptance criteria
- [ ] After screen-off → screen-on, the status dot updates within one polling interval when the phone pauses/resumes
- [ ] No coroutine leaks after `onDestroy`

## Out of scope
The relaunch loop (ticket 006).
