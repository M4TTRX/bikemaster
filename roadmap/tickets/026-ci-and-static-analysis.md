---
id: "026"
title: CI workflow and static analysis
type: ci
priority: P1
status: todo
area: repo
effort: S
audit_ref: [H9, A2]
depends_on: ["002"]
assignee:
---

# 026 — CI workflow and static analysis

## Problem
The repository has no continuous integration pipeline (no `.github/workflows`), no linter configuration (detekt / ktlint), and no automated test execution on pull requests. Regressions, broken toolchains, or syntax errors can be merged without any verification gate.

## Evidence
- Absence of `.github/` folder in repo root.

## Proposed fix
1. Create `.github/workflows/ci.yml`:
   - Triggers on push to `main` and pull requests.
   - Sets up JDK 17 using standard actions (`actions/setup-java@v4`).
   - Runs `./gradlew testDebugUnitTest lintDebug`.
   - Uploads test and lint reports on failure.
2. Add ktlint or detekt gradle plugin to enforce Kotlin code hygiene and prevent swallowed exceptions / wildcard imports.

## Acceptance criteria
- [ ] GitHub Actions CI workflow file in place.
- [ ] Pull requests and pushes automatically run unit tests and Android lint.
- [ ] Clean build and test execution on Ubuntu runners.

## Out of scope
Automated Google Play deployment / release CD pipeline.
