# Velo Roadmap & Ticket Backlog

This folder holds work tickets as plain Markdown files, so humans and AI agents can pick them up without an external tracker.
The initial backlog comes from the codebase audit of 2026-10-05 (security, hygiene, clarity, documentation, AI-agent readiness).

- Product roadmap for the watch app: [`../WEAR_OS_ROADMAP.md`](../WEAR_OS_ROADMAP.md)
- Ticket template: [`TEMPLATE.md`](TEMPLATE.md)
- Tickets: [`tickets/`](tickets/)

## How to pick up a ticket

1. Choose the lowest-numbered ticket with `status: todo` at the highest priority (`P0` → `P1` → `P2`) whose `depends_on` tickets are all `done`.
2. Set `status: in-progress` and fill in `assignee` in the frontmatter, and commit that change on its own.
3. Do the work on a branch named `ticket/<id>-<slug>` (e.g. `ticket/004-wheel-distance`).
4. Meet **every** acceptance criterion. Unit tests and lint must pass (see ticket 002 for the build setup).
5. Set `status: done`, add a short `## Resolution` section (what changed, PR/commit link), and update the index table below.

### Status values
`todo` · `in-progress` · `blocked` · `done` · `wontfix`

### Priority
- **P0**: wrong ride data, battery drain, or blocks building/publishing. Do it now.
- **P1**: security/privacy, docs, and the test and CI checks that let us verify changes.
- **P2**: maintainability, refactors, polish.

## Decisions / accepted behaviour

| Topic | Decision |
| :--- | :--- |
| Mock simulator on physical devices | **Intended.** When `mock_simulator_enabled` is on, `DefaultRideRepository` uses simulator telemetry on any device, ignoring real sensor input. Do **not** "fix" this (audit finding B5 was rejected). The ViewModel's emulator gate on start/resume is a separate concern. |
| Shared `applicationId` across `:app` and `:wear` | **Required** for Wearable Data Layer routing. Keep it. |

## Index

| ID | Title | Type | Priority | Status | Audit ref |
| :-- | :-- | :-- | :-- | :-- | :-- |
| [001](tickets/001-commit-staged-work.md) | Commit staged work in logical slices | chore | P0 | done | H1 |
| [002](tickets/002-portable-jdk-toolchain.md) | Replace hard-coded JDK path with Gradle toolchain | build | P0 | todo | H2, A2 |
| [003](tickets/003-wear-version-code.md) | Distinct wear `versionCode` and aligned SDK targets | build | P0 | todo | H4 |
| [004](tickets/004-wheel-distance-accumulation.md) | Fix wheel-sensor distance under/double counting | bug | P0 | todo | B1 |
| [005](tickets/005-average-power-integration.md) | Fix average power calculation | bug | P0 | todo | B2 |
| [006](tickets/006-watch-relaunch-loop.md) | Stop watch relaunching every 3 s while recording | bug | P0 | todo | B3 |
| [007](tickets/007-watch-polling-cancelled-scope.md) | Watch status polling dies after first `onStop` | bug | P0 | todo | B4 |
| [008](tickets/008-elevation-gain-noise.md) | Elevation gain inflated by GPS altitude noise | bug | P1 | todo | B6 |
| [009](tickets/009-foreground-service-hardening.md) | Harden `RideTrackingService` | bug | P1 | todo | B7 |
| [010](tickets/010-wear-capability-listener-leak.md) | Capability listener leak & stale `isConnected` | bug | P2 | todo | B8 |
| [011](tickets/011-paired-sensor-reconnect-storm.md) | Paired sensors reconnect on every settings change | bug | P2 | todo | B9 |
| [012](tickets/012-ride-pipeline-thread-safety.md) | Thread-safety of ride pipeline mutable state | bug | P2 | todo | B10 |
| [013](tickets/013-wear-permission-prompt-loop.md) | Watch permission prompt loop | bug | P2 | todo | B11 |
| [014](tickets/014-structured-paired-device-storage.md) | Structured, validated paired-device storage | security | P1 | todo | S1 |
| [015](tickets/015-remove-health-data-logs.md) | Remove health data from logs | security | P1 | todo | S2 |
| [016](tickets/016-trim-permissions.md) | Remove unused / over-broad permissions | security | P1 | todo | S3 |
| [017](tickets/017-enable-r8-release.md) | Enable R8 for release builds | security | P1 | todo | S4 |
| [018](tickets/018-backup-rules.md) | Define backup / data-extraction rules | security | P2 | todo | S5 |
| [019](tickets/019-validate-wear-payloads.md) | Validate Wear Data Layer payloads | security | P2 | todo | S6 |
| [020](tickets/020-ble-scan-filters.md) | Use BLE `ScanFilter`s for sensor discovery | perf | P2 | todo | S7 |
| [021](tickets/021-dependency-cleanup.md) | Dependency cleanup | chore | P2 | todo | H3 |
| [022](tickets/022-no-swallowed-exceptions.md) | Stop swallowing exceptions silently | refactor | P2 | todo | H5 |
| [023](tickets/023-deduplicate-code.md) | Remove duplicated code and magic numbers | refactor | P2 | todo | H6, H10 |
| [024](tickets/024-isolate-demo-code.md) | Isolate emulator/demo code from production paths | refactor | P2 | todo | H7 |
| [025](tickets/025-meaningful-unit-tests.md) | Unit tests for parsing, persistence and accumulation | test | P1 | todo | H8, A2 |
| [026](tickets/026-ci-and-static-analysis.md) | CI workflow + static analysis | ci | P1 | todo | H9, A2 |
| [027](tickets/027-split-large-files.md) | Split `SettingsScreen` and other large files | refactor | P2 | todo | C1, A5 |
| [028](tickets/028-uistate-uievent.md) | Introduce `UiState` / `UiEvent` | refactor | P2 | todo | C2 |
| [029](tickets/029-split-ble-sensor-manager.md) | Extract `SensorClassifier` from `BleSensorManager` | refactor | P2 | todo | C3 |
| [030](tickets/030-shared-wear-contract-module.md) | `:shared` module for the phone↔watch contract | refactor | P2 | todo | C4 |
| [031](tickets/031-naming-and-nonnull-deps.md) | Fix misleading names; non-null settings dependency | refactor | P2 | todo | C5 |
| [032](tickets/032-string-resources.md) | Move UI strings to resources | i18n | P2 | todo | C6 |
| [033](tickets/033-kdoc-public-apis.md) | KDoc for public APIs and GATT parsing | docs | P2 | todo | C7 |
| [034](tickets/034-readme.md) | Add project `README.md` | docs | P1 | todo | D1 |
| [035](tickets/035-wear-protocol-spec.md) | Document phone↔watch protocol | docs | P1 | todo | D2 |
| [036](tickets/036-correct-wear-roadmap.md) | Correct `WEAR_OS_ROADMAP.md` | docs | P1 | todo | D3 |
| [037](tickets/037-architecture-doc.md) | Phone data-pipeline architecture doc | docs | P2 | todo | D4 |
| [038](tickets/038-license-changelog-privacy.md) | LICENSE, CHANGELOG, privacy note | docs | P2 | todo | D5 |
| [039](tickets/039-agents-md.md) | Add root `AGENTS.md` | agents | P1 | todo | A1, A4 |
| [040](tickets/040-align-oled-skill.md) | Align `compose-oled-ui` skill with code | agents | P1 | todo | A3 |
