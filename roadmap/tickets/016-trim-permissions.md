---
id: "016"
title: Remove unused / over-broad permissions
type: security
priority: P1
status: todo
area: app, wear
effort: S
audit_ref: [S3]
depends_on: []
assignee:
---

# 016 — Remove unused / over-broad permissions

## Problem
Both manifests declare permissions that are never requested at runtime, never exercised by code, or violate Google Play policy for this app category:
- `:app`:
  - `ACCESS_BACKGROUND_LOCATION`: Never requested at runtime. A foreground service with `FOREGROUND_SERVICE_LOCATION` already has location access without triggering strict Google Play background location policy reviews.
  - `WAKE_LOCK`: The app uses Compose / window flag `FLAG_KEEP_SCREEN_ON`, making low-level wake locks redundant.
- `:wear`:
  - `FOREGROUND_SERVICE_HEALTH` & `FOREGROUND_SERVICE`: Declared but no foreground service exists in `:wear`.
  - `USE_FULL_SCREEN_INTENT`: High-risk permission restricted by Google Play on Android 14+ to calling and alarm apps; will trigger Play Console rejection for a companion fitness app.

## Evidence
- [`app/src/main/AndroidManifest.xml#L7`](../../app/src/main/AndroidManifest.xml): `ACCESS_BACKGROUND_LOCATION`
- [`app/src/main/AndroidManifest.xml#L22`](../../app/src/main/AndroidManifest.xml): `WAKE_LOCK`
- [`wear/src/main/AndroidManifest.xml#L10-L12`](../../wear/src/main/AndroidManifest.xml): `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`

## Proposed fix
1. Remove `ACCESS_BACKGROUND_LOCATION` and `WAKE_LOCK` from `app/src/main/AndroidManifest.xml`.
2. Remove `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, and `USE_FULL_SCREEN_INTENT` from `wear/src/main/AndroidManifest.xml`.
3. In `WearOsDataListenerService.kt`, launch `MainActivity` via standard high-priority notifications without requiring full-screen intent privileges.

## Acceptance criteria
- [ ] Unused permissions removed from both manifests.
- [ ] Ride tracking foreground service in `:app` still receives GPS when screen is locked.
- [ ] Screen stays awake while handlebar dashboard is active.

## Out of scope
Migrating watch to Health Services (future roadmap).
