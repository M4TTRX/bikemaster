---
id: "011"
title: Paired sensors reconnect on every settings change
type: bug
priority: P2
status: todo
area: app
effort: S
audit_ref: [B9]
depends_on: []
assignee:
---

# 011 — Paired sensors reconnect on every settings change

## Problem
`pairedBleDevicesFlow` is derived from the whole DataStore, so it emits on **any** preference change (theme, units, tiles, ...). Each emission calls `connectDevice` for every saved sensor. Side effects:
- `disconnectSensor()` is undone by the next unrelated settings change.
- Disconnected clients are torn down and rebuilt again and again.

## Evidence
- [`RideRepository.kt#L215-L221`](../../app/src/main/java/com/velo/app/data/RideRepository.kt)
- [`SettingsRepository.kt#L122-L165`](../../app/src/main/java/com/velo/app/data/SettingsRepository.kt)
- [`RideRepository.kt#L463-L465`](../../app/src/main/java/com/velo/app/data/RideRepository.kt): `disconnectSensor`

## Proposed fix
- Add `.distinctUntilChanged()` to the settings flows (all of them, not just this one).
- Diff the paired set: connect only newly added addresses and disconnect removed ones. Keep a "user-disconnected" set in memory that suppresses auto-reconnect until the user reconnects or the app restarts.

## Acceptance criteria
- [ ] Changing theme/units doesn't call `connectDevice`
- [ ] A sensor disconnected via `disconnectSensor` stays disconnected until explicitly reconnected
- [ ] Unit test with a fake `BleSensorManager` verifies the diff behaviour

## Out of scope
Storage format (ticket 014).
