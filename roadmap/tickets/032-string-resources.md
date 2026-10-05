---
id: "032"
title: Move UI strings to resources
type: i18n
priority: P2
status: todo
area: app
effort: M
audit_ref: [C6]
depends_on: []
assignee:
---

# 032 — Move UI strings to resources

## Problem
`:app` has no `res/values/strings.xml` file. Every single label, dialog prompt, and unit abbreviation is hardcoded as inline Kotlin string literals. Domain enums (`MetricType`, `SpeedSource`, `SensorDataType`) hardcode English user-facing strings directly in model definitions (e.g. `title = "Auto (Sensor Priority)"`). This prevents internationalization (i18n), localization, and dynamic formatting.

## Evidence
- `ls app/src/main/res/values/strings.xml` -> file does not exist.
- [`RideTelemetry.kt#L22-L47`](../../app/src/main/java/com/velo/app/model/RideTelemetry.kt): Hardcoded English `title` properties.

## Proposed fix
1. Create `app/src/main/res/values/strings.xml`.
2. Move all dashboard and settings text, descriptions, unit labels, and dialog titles to XML string resources.
3. Replace enum `title: String` with `@StringRes val titleRes: Int`, or provide a mapping in the UI presentation layer.
4. Replace raw Compose `Text("...")` with `Text(stringResource(R.string....))`.

## Acceptance criteria
- [ ] `app/src/main/res/values/strings.xml` exists and contains all user-facing strings.
- [ ] Domain enums do not store localized UI presentation strings.

## Out of scope
Translating strings to non-English languages (initial pass creates English default `strings.xml`).
