---
id: "014"
title: Structured, validated paired-device storage
type: security
priority: P1
status: todo
area: app
effort: M
audit_ref: [S1]
depends_on: []
assignee:
---

# 014 — Structured, validated paired-device storage

## Problem
BLE sensor metadata is stored in DataStore as a pipe-and-comma delimited string:
`address|name|detectedCaps|enabledCaps`

Because device names are broadcasted over the air by arbitrary untrusted peripherals:
1. An advertised device name containing `|` or `,` corrupts the serialized record and breaks parsing.
2. Updates and deletions use `encoded.contains(address)` which matches substrings in other device names, potentially deleting or mutating the **wrong device**.
3. Unbounded name length allows bloating Preferences DataStore memory.

## Evidence
- [`SettingsRepository.kt#L122-L165`](../../app/src/main/java/com/velo/app/data/SettingsRepository.kt): Deserialization splits by `|`.
- [`SettingsRepository.kt#L230-L296`](../../app/src/main/java/com/velo/app/data/SettingsRepository.kt): `contains(device.address)` string check; manual string formatting.

## Proposed fix
1. Migrate storage to a structured format:
   - Prefer `kotlinx.serialization.json` string in DataStore, or Proto DataStore.
2. Sanitize and validate before persisting:
   - Validate MAC address format (`^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$`).
   - Truncate names to maximum 32 characters; strip non-printable/control characters.
3. Replace substring checks with exact `address == targetAddress` equality.
4. Provide backward migration for legacy 3-part and 4-part pipe-delimited records.

## Acceptance criteria
- [ ] Device named `Sensor|Hacked,Evil` saves, loads, and unpairs cleanly without breaking DataStore parsing.
- [ ] Two devices where one address is a substring of another's name do not conflict on removal.
- [ ] Unit tests for serialization, deserialization, sanitization, and migration from legacy pipe strings.

## Out of scope
Remote cloud sync of paired sensors.
