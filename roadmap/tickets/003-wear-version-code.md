---
id: "003"
title: Distinct wear versionCode and aligned SDK targets
type: build
priority: P0
status: todo
area: build
effort: S
audit_ref: [H4]
depends_on: []
assignee:
---

# 003 — Distinct wear versionCode and aligned SDK targets

## Problem
`:app` and `:wear` intentionally share `applicationId = com.velo.app` (required for Data Layer routing, see the roadmap README decisions). But they also share `versionCode = 1`. Google Play rejects two artifacts with the same package and version code in one release. The SDK targets also differ (35 vs 34).

## Evidence
- [`app/build.gradle.kts#L12-L16`](../../app/build.gradle.kts): `applicationId "com.velo.app"`, `targetSdk 35`, `versionCode 1`
- [`wear/build.gradle.kts#L12-L16`](../../wear/build.gradle.kts): `applicationId "com.velo.app"`, `targetSdk 34`, `versionCode 1`

## Proposed fix
- Use a versioning scheme with a form-factor offset, e.g. wear `versionCode = 1_000_000 + baseVersionCode`. Define the base in `gradle.properties` or the version catalog so both modules derive from one place.
- Align `targetSdk` (35 if the Wear OS 5+ requirements are met; otherwise document why wear lags).
- Add a comment above `applicationId` in `wear/build.gradle.kts` explaining that it must stay equal to the phone's.

## Acceptance criteria
- [ ] `versionCode` differs between modules and comes from a single source
- [ ] `targetSdk` is aligned, or the difference is documented in a comment
- [ ] `applicationId` invariant is commented in both build files

## Out of scope
Play Console setup and signing configuration.
