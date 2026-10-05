---
id: "021"
title: Dependency cleanup
type: chore
priority: P2
status: todo
area: build
effort: S
audit_ref: [H3]
depends_on: []
assignee:
---

# 021 — Dependency cleanup

## Problem
The Gradle build files contain unused and non-standard dependency declarations:
- `:app` includes `androidx.fragment:fragment-ktx`, but the app is 100% Jetpack Compose using `ComponentActivity`.
- `:wear` includes `androidx.health:health-services-client`, but heart rate streaming currently uses standard Android `SensorManager`.
- `:app` declares `androidx.wear:wear-remote-interactions:1.1.0` as an inline hardcoded string bypassing `libs.versions.toml`.
- `:wear` uses `wear-compose-material3:1.0.0-alpha30`, an early preview.

## Evidence
- [`app/build.gradle.kts#L47`](../../app/build.gradle.kts): `implementation(libs.androidx.fragment.ktx)`
- [`app/build.gradle.kts#L62`](../../app/build.gradle.kts): `implementation("androidx.wear:wear-remote-interactions:1.1.0")`
- [`wear/build.gradle.kts#L61`](../../wear/build.gradle.kts): `implementation(libs.androidx.health.services.client)`

## Proposed fix
1. Remove `androidx.fragment.ktx` from `:app` and `libs.versions.toml`.
2. Move `wear-remote-interactions` into `libs.versions.toml`.
3. Remove unused `health-services-client` from `:wear`, or keep it if Health Services migration is actively underway.
4. Clean up unused version definitions in `gradle/libs.versions.toml`.

## Acceptance criteria
- [ ] No unreferenced libraries in `build.gradle.kts`.
- [ ] All dependencies managed through `gradle/libs.versions.toml`.
- [ ] App and wear compile cleanly.

## Out of scope
Upgrading Compose BOM version.
