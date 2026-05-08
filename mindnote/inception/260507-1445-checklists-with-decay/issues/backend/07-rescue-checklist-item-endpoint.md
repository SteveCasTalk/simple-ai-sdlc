---
type: issue
feature: checklists-with-decay
lane: backend
status: ready
wave: 2
estimate: 30m
blocked-by:
  - "[[02-create-checklist-item-endpoint]]"
tags:
  - inception/issue
  - lane/backend
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [BE] `POST /notes/{id}/items/{item_id}/rescue` — un-archive + reset timer

**Lane:** Backend
**PRD section:** [[PRD#Story 4 — Auto-archive forces a rescue moment]], [[PRD#Goals]] G5.
**API contract section:** [[api-contract#POST /notes/{id}/items/{item_id}/rescue]].

## Why

Without rescue, archive is a one-way trip — the user has no recourse if the cron buried something they still cared about. Rescue is also the one user gesture that explicitly says "I disagree with the auto-archive verdict; this thing matters."

## Acceptance criteria

- [ ] `POST /notes/{id}/items/{item_id}/rescue` (empty body) on an archived item: clears `archived_at` (sets to `NULL`), sets `last_touched_at = now`. Returns 200 with the full updated item block, which now has `archived_at: null` and a fresh `last_touched_at`.
- [ ] Idempotency / no-op behavior: rescue on a non-archived item returns 200 with `last_touched_at = now` and `archived_at: null` (already was). The endpoint doubles as a manual "touch."
- [ ] Rescue on a non-existent item or note returns 404.
- [ ] After rescue, `GET /notes/{id}` (default — exclude-archived) returns the item in `body` with full freshness.
- [ ] Test: archive an item by directly setting `archived_at` to past time; call rescue; assert `archived_at == null` and `last_touched_at` was advanced. Call rescue again on the same now-live item; assert `last_touched_at` advanced again.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[02-create-checklist-item-endpoint]] — items must exist with `archived_at` field (added in story 01's foundation).

## Hints (non-binding)

- **Reuse the touch helper** from story 03 if Construction made one — rescue is just `archived_at = null` then `touch()`.
- **Watch out for:** clients may call rescue when they meant to PATCH (e.g. an archived item the user wants to edit). The mob's [[open-questions#Q4]] default is: PATCH on archived = 409, force the client to call rescue first. Keep that boundary clean here — rescue does *not* accept any body fields; it's a pure restore.

## Out of scope for this story

- Bulk rescue of all archived items in a note.
- Rescue reverting the archived item to its pre-archive `last_touched_at` (we explicitly choose to set it to `now`, per [[decisions#D5]] and PRD G5).
