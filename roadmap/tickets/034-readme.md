---
id: "034"
title: Add project README.md
type: docs
priority: P1
status: todo
area: docs
effort: S
audit_ref: [D1]
depends_on: []
assignee:
---

# 034 — Add project README.md

## Problem
The root of the repository lacks a `README.md`. There is no introduction to what Velo is, what prerequisites are needed to build it, how to run tests, how the emulator mock simulator works, or what architecture it follows.

## Evidence
- `ls README.md` -> file not found.

## Proposed fix
Create a comprehensive `README.md` containing:
1. **Project Overview**: Modern Android & Wear OS bike computer designed for true pitch-black OLED screens.
2. **Key Capabilities**: Bluetooth LE sensors (CSC speed/cadence, HR, Power meters), Wear OS wrist PPG companion, FusedLocation GPS, zero-power OLED styling.
3. **Module Structure**: Overview of `:app` (phone) and `:wear` (smartwatch companion).
4. **Development Prerequisites**: JDK 17, Android SDK 35, Gradle setup.
5. **Building & Running**: Gradle command examples for compiling, running unit tests, and linting.
6. **Mock Simulator Guide**: How to test telemetry and sensor dynamics on Android emulator without hardware.

## Acceptance criteria
- [ ] Root `README.md` created and formatted with GitHub Flavored Markdown.
- [ ] Build and test commands documented accurately.

## Out of scope
Detailed protocol specification (covered in Ticket 035).
