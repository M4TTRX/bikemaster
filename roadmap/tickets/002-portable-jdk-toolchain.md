---
id: "002"
title: Replace hard-coded JDK path with Gradle toolchain
type: build
priority: P0
status: todo
area: build
effort: S
audit_ref: [H2, A2]
depends_on: []
assignee:
---

# 002 — Replace hard-coded JDK path with Gradle toolchain

## Problem
`gradle.properties` commits a machine-specific JDK location. Builds fail on any machine without Homebrew `openjdk@17` at that exact path, including CI runners and AI-agent sandboxes. Without a portable build, no other ticket can be checked automatically.

## Evidence
- [`gradle.properties`](../../gradle.properties): `org.gradle.java.home=/opt/homebrew/opt/openjdk@17`
- [`app/build.gradle.kts#L39-L43`](../../app/build.gradle.kts) and [`wear/build.gradle.kts#L39-L43`](../../wear/build.gradle.kts) set `jvmTarget` manually.

## Proposed fix
- Remove `org.gradle.java.home` from `gradle.properties`. If needed locally, set it in `~/.gradle/gradle.properties`.
- Add `kotlin { jvmToolchain(17) }` in both modules (optionally with the `org.gradle.toolchains.foojay-resolver-convention` plugin in `settings.gradle.kts` for auto-provisioning).
- Document the JDK requirement in the README (ticket 034).

## Acceptance criteria
- [ ] No absolute paths in committed Gradle files
- [ ] `./gradlew assembleDebug testDebugUnitTest` succeeds on a machine with only `JAVA_HOME` set to any JDK 17+
- [ ] Ticket 026's CI job uses this setup

## Out of scope
Upgrading AGP/Kotlin versions.
