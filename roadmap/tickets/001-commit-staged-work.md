---
id: "001"
title: Commit staged work in logical slices
type: chore
priority: P0
status: done
area: repo
effort: S
audit_ref: [H1]
depends_on: []
assignee: matthieu.roux
---

# 001 — Commit staged work in logical slices

## Problem
28 files are staged but uncommitted (+1851/−468 lines), covering theme mode, button groups, haptics, metric icons, sensor changes and a wear resource. Every other ticket touches these files. Starting work on top of them risks lost work and makes changes impossible to review.

## Resolution
Committed in `252f87b` (`fix(ble): resolve speed dropouts, improve reconnection lifecycle, and support Van Rysel single-mode sensor`). Git working directory is clean.


## Evidence
- `git status --short` shows 28 `M`/`A` entries across `app/` and `wear/`.

## Proposed fix
Split into reviewable conventional commits, for example:
1. `feat(theme): add ThemeMode (system/light/dark) setting`: `ThemeMode.kt`, `Theme.kt`, `Color.kt`, `SettingsRepository`, `MainActivity`
2. `feat(ui): add SingleSelectButtonGroup, haptics and metric icons`: `ButtonGroup.kt`, `Haptics.kt`, `MetricIcons.kt`, consumers
3. `refactor(sensors): ...` for the BLE/Wear manager changes
4. `test: ...` for test updates

## Acceptance criteria
- [ ] `git status` is clean (except the new `roadmap/` folder if it's committed separately)
- [ ] Each commit builds on its own

## Out of scope
Any functional change.
