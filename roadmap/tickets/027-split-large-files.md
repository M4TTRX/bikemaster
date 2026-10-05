---
id: "027"
title: Split SettingsScreen and other large files
type: refactor
priority: P2
status: todo
area: app
effort: M
audit_ref: [C1, A5]
depends_on: []
assignee:
---

# 027 — Split SettingsScreen and other large files

## Problem
`SettingsScreen.kt` is over 800 lines of code with a single top-level Composable spanning 555 lines. Similarly, `TileCustomizationSheet.kt` is 404 lines, `BleSensorManager.kt` is 446 lines, and `RideRepository.kt` is 471 lines. Very large monolithic files degrade IDE performance, increase cognitive load, complicate code review, and burn excessive context tokens for AI coding agents.

## Evidence
- [`SettingsScreen.kt#L82-L637`](../../app/src/main/java/com/velo/app/ui/settings/SettingsScreen.kt): Single monolithic function.

## Proposed fix
Decompose `SettingsScreen.kt` into modular composables within a `ui/settings/sections/` package:
1. `MeasurementsSection.kt`: Units, speed source, wheel preset picker.
2. `BleSensorsSection.kt`: Paired sensor cards, scanning CTA, capability chips.
3. `WearOsSection.kt`: Watch companion connection state, Play Store install CTA.
4. `CockpitAppearanceSection.kt`: Theme mode, keep screen on.
5. `DeveloperSection.kt`: Mock simulator switch.

## Acceptance criteria
- [ ] No single file in `ui/settings/` exceeds 250 lines.
- [ ] Each settings section is an independently previewable Composable.
- [ ] Behavior and OLED styling remain unchanged.

## Out of scope
Redesigning the settings UI.
