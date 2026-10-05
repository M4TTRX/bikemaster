---
id: "040"
title: Align compose-oled-ui skill with code
type: agents
priority: P1
status: todo
area: agents
effort: S
audit_ref: [A3]
depends_on: []
assignee:
---

# 040 — Align compose-oled-ui skill with code

## Problem
The agent skill [`.agents/skills/compose-oled-ui/SKILL.md`](../../.agents/skills/compose-oled-ui/SKILL.md) contradicts the codebase in two notable areas:
1. **Thread safety**: The skill recommends a non-thread-safe cache `private val fontCache = mutableMapOf<Pair<Float, Int>, FontFamily>()`, whereas the actual production code in `Theme.kt` and `Type.kt` correctly uses `ConcurrentHashMap`. An agent following the skill might erroneously "simplify" the code to an unsafe map.
2. **Throttling rule**: The skill states that telemetry emissions are throttled to 1Hz using `sample(1000)` or `throttleFirst`, which the actual pipeline does not currently employ.

## Evidence
- [`.agents/skills/compose-oled-ui/SKILL.md#L41`](../../.agents/skills/compose-oled-ui/SKILL.md): `mutableMapOf` snippet.
- [`app/src/main/java/com/velo/app/ui/theme/Theme.kt#L81`](../../app/src/main/java/com/velo/app/ui/theme/Theme.kt): `ConcurrentHashMap`.

## Proposed fix
1. Update `.agents/skills/compose-oled-ui/SKILL.md` section 2 to recommend `ConcurrentHashMap` for font family caching.
2. Clarify section 3 on telemetry throttling to match actual repository architecture (1Hz timer loop + sensor flows).

## Acceptance criteria
- [ ] Code snippets in `SKILL.md` are thread-safe and match current production practices.
- [ ] Telemetry pipeline description in `SKILL.md` aligns with implementation.

## Out of scope
Adding new skills.
