---
id: "018"
title: Define backup and data extraction rules
type: security
priority: P2
status: todo
area: app
effort: S
audit_ref: [S5]
depends_on: ["014"]
assignee:
---

# 018 — Define backup and data extraction rules

## Problem
`allowBackup="true"` is enabled without `dataExtractionRules` (Android 12+) or `fullBackupContent` (legacy). Paired sensor Bluetooth MAC addresses, user preferences, and saved tile configurations are included in Google cloud backups and adb backups by default, which can lead to unexpected state restoration across different hardware devices.

## Evidence
- [`app/src/main/AndroidManifest.xml#L26`](../../app/src/main/AndroidManifest.xml): `android:allowBackup="true"`
- Missing `data_extraction_rules.xml` and `backup_rules.xml` in `res/xml/`.

## Proposed fix
1. Create `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml`.
2. Explicitly specify which DataStore files are backed up (e.g., user preferences such as units and theme) versus excluded (paired hardware MAC addresses which may not match a new phone or transferred device).
3. Reference rules in `AndroidManifest.xml` via `android:dataExtractionRules` and `android:fullBackupContent`.

## Acceptance criteria
- [ ] XML backup rules configured according to Android 12+ specifications.
- [ ] Paired hardware addresses excluded from cloud device transfer if hardware-specific.

## Out of scope
Custom cloud backup provider.
