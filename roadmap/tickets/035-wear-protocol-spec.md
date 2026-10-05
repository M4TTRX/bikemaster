---
id: "035"
title: Document phone-watch protocol
type: docs
priority: P1
status: todo
area: docs
effort: S
audit_ref: [D2]
depends_on: []
assignee:
---

# 035 — Document phone-watch protocol

## Problem
The communication contract between `:app` and `:wear` over Google Play Services Wearable Data Layer exists only implicitly in code across two modules. There is no written specification documenting the paths, payloads, frequency, direction, or sequencing.

## Evidence
- Paths `/velo/heart_rate`, `/velo/status`, `/velo/open_companion`, `/velo/status_request` implemented without protocol documentation.

## Proposed fix
Create `docs/wear-protocol.md`:
1. **Overview**: Wearable Data Layer MessageClient architecture between handheld and watch.
2. **Endpoints Table**:
   | Path | Direction | Payload | Rate | Description |
   | :--- | :--- | :--- | :--- | :--- |
   | `/velo/heart_rate` | Watch -> Phone | 4-byte big-endian Int (BPM) | ~1Hz | Real-time optical HR |
   | `/velo/status` | Phone -> Watch | UTF-8 String (`RECORDING`, `PAUSED`, `IDLE`, `FINISHED`) | On state change | Ride recording state |
   | `/velo/status_request` | Watch -> Phone | 1 byte flag | On connect / periodic | Sync request |
   | `/velo/open_companion` | Phone -> Watch | 1 byte flag | On ride start | Remote wake-up |
3. **Sequence Diagrams**: Mermaid diagrams depicting connection handshake and ride lifecycle sync.

## Acceptance criteria
- [ ] `docs/wear-protocol.md` created with complete endpoint specifications and payload formats.
- [ ] Linked from `WEAR_OS_ROADMAP.md` and `README.md`.

## Out of scope
Protocol refactoring into a shared module (covered in Ticket 030).
