---
id: "038"
title: Add LICENSE, CHANGELOG and privacy note
type: docs
priority: P2
status: todo
area: repo
effort: S
audit_ref: [D5]
depends_on: []
assignee:
---

# 038 — Add LICENSE, CHANGELOG and privacy note

## Problem
The repository is missing standard open-source governance and release files:
- No `LICENSE` file.
- No `CHANGELOG.md` documenting versions, migrations, or release milestones.
- No privacy disclosure regarding how user GPS location data and optical heart rate metrics are processed and retained locally.

## Evidence
- `ls LICENSE CHANGELOG.md PRIVACY.md` -> none exist.

## Proposed fix
1. Add `LICENSE` file (e.g. Apache 2.0 or MIT as determined by project maintainer).
2. Create `CHANGELOG.md` using "Keep a Changelog" format initialized with v1.0.0 milestone.
3. Create `PRIVACY.md` documenting that:
   - Location, BLE, and heart rate telemetry are processed strictly on-device.
   - Zero telemetry data is transmitted to remote third-party servers.

## Acceptance criteria
- [ ] `LICENSE`, `CHANGELOG.md`, and `PRIVACY.md` exist in repo root.

## Out of scope
Formal legal privacy policy for Google Play Console web hosting.
