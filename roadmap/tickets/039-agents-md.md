---
id: "039"
title: Add root AGENTS.md
type: agents
priority: P1
status: todo
area: agents
effort: S
audit_ref: [A1, A4]
depends_on: ["002"]
assignee:
---

# 039 — Add root AGENTS.md

## Problem
AI coding assistants and agentic tools currently lack an onboarding guide (`AGENTS.md`). Every session requires exploring the entire codebase to understand module hierarchy, build commands, hardware invariants, testing runbooks, and domain constraints. Crucial rules (e.g., that `:app` and `:wear` must share `applicationId`, that mock simulator behavior is intentional, and that the pure-black OLED style guide must be followed) can easily be violated by automated agents.

## Evidence
- No `AGENTS.md` in repository root.

## Proposed fix
Create `AGENTS.md`:
1. **Project Summary & Architecture**: Purpose of Velo, modules (`:app`, `:wear`, roadmap backlog).
2. **Key Commands**: Build, test, lint, and run commands with Gradle.
3. **Core Invariants & Non-Negotiables**:
   - Shared `applicationId` requirement for Wearable Data Layer routing.
   - Mock simulator behavior: deliberate inclusion on real devices (decision documented in `roadmap/README.md`).
   - Pure pitch-black OLED background (`#000000`) for bike dashboard.
   - Zero border policy on OLED cockpit elements.
4. **Codebase Conventions**: StateFlow exposure patterns, no swallowed exceptions, Compose lifecycle patterns.
5. **Skill Integration**: Reference `.agents/skills/compose-oled-ui/SKILL.md`.

## Acceptance criteria
- [ ] `AGENTS.md` created in root directory.
- [ ] Invariants clearly stated with do's and don'ts.

## Out of scope
Tool-specific configuration files.
