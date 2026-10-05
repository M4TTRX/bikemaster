---
id: "036"
title: Correct WEAR_OS_ROADMAP.md
type: docs
priority: P1
status: todo
area: docs
effort: S
audit_ref: [D3]
depends_on: ["006"]
assignee:
---

# 036 — Correct WEAR_OS_ROADMAP.md

## Problem
`WEAR_OS_ROADMAP.md` contains several factual inaccuracies and misleading claims:
1. Claims heart rate streaming is throttled to 1Hz, but `WearCopilotManager` forwards raw events directly on every `onSensorChanged(SENSOR_DELAY_NORMAL)` without throttling.
2. Claims status is "Deployed Live", but the project has no automated release pipeline or Play Store track setup.
3. Describes auto-launch via full-screen intent as fully verified without documenting the 3-second relaunch notification loop (Ticket 006).
4. Mermaid diagram displays an invalid bidirectional edge `Repo -- ... --> WearReceiver`.

## Evidence
- [`WEAR_OS_ROADMAP.md#L9`](../../WEAR_OS_ROADMAP.md): "Status: Deployed Live"
- [`WEAR_OS_ROADMAP.md#L14`](../../WEAR_OS_ROADMAP.md): "streams over /velo/heart_rate at 1Hz"

## Proposed fix
1. Update status to reflect reality: "In active development / Local test builds verified on hardware".
2. Accurately describe optical HR sampling rates and any throttling behavior.
3. Fix the Mermaid diagram in section 2 to depict clear, accurate directional data flow.
4. Update the roadmap phase list with completed vs upcoming items.

## Acceptance criteria
- [ ] `WEAR_OS_ROADMAP.md` accurately describes actual implemented behavior.
- [ ] Mermaid syntax validated and rendering cleanly.

## Out of scope
Adding new Wear features.
