---
id: "017"
title: Enable R8 for release builds
type: security
priority: P1
status: todo
area: build
effort: S
audit_ref: [S4]
depends_on: []
assignee:
---

# 017 — Enable R8 for release builds

## Problem
`isMinifyEnabled` is set to `false` in both `:app` and `:wear` release build types. Production APKs ship un-obfuscated with full symbols, larger binary size, and unused code. Furthermore, `:wear/proguard-rules.pro` does not even exist despite being referenced in `wear/build.gradle.kts`.

## Evidence
- [`app/build.gradle.kts#L23`](../../app/build.gradle.kts): `isMinifyEnabled = false`
- [`wear/build.gradle.kts#L23`](../../wear/build.gradle.kts): `isMinifyEnabled = false`
- `wear/proguard-rules.pro`: Missing file.
- [`app/proguard-rules.pro#L4`](../../app/proguard-rules.pro): Overly broad `-keep class no.nordicsemi.android.ble.** { *; }`

## Proposed fix
1. Create `wear/proguard-rules.pro` with necessary rules for Wear Compose and Health/Wearable APIs.
2. Tighten `app/proguard-rules.pro` keeping only required public APIs and `@Keep` annotated models.
3. Enable `isMinifyEnabled = true` and `isShrinkResources = true` in `release` blocks of both `:app` and `:wear`.
4. Add R8 rules to strip `Log.v` and `Log.d` calls in release builds.

## Acceptance criteria
- [ ] `./gradlew assembleRelease` succeeds for both `:app` and `:wear`.
- [ ] APK size is measurably reduced.
- [ ] Nordic BLE and DataStore deserialization function correctly in release mode.

## Out of scope
App signing / key management.
