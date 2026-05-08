---
type: issue
feature: checklists-with-decay
lane: backend
status: ready
wave: 1
estimate: 45m
blocked-by:
  - "[[01-block-shaped-notes-foundation]]"
tags:
  - inception/issue
  - lane/backend
  - feature/checklists-with-decay
  - status/ready
  - wave/1
---

# [BE] `POST /notes/{id}/items` — append a new checklist item

**Lane:** Backend
**PRD section:** [[PRD#Story 1 — Capture a thought-then-task as you type]].
**API contract section:** [[api-contract#POST /notes/{id}/items — append a new item]].

## Why

This is the only path to bring an item into existence. Without it, the foundation from story 01 is half a feature — a note can render blocks but no one can add an item.

## Acceptance criteria

- [ ] `POST /notes/{id}/items` with `{ "text": "Call dentist" }` returns 201 and an item block whose `id` is server-issued, `text` matches, `done = false`, `last_touched_at` is the response time, `archived_at = null`.
- [ ] `position` query/body field (optional): if omitted, the new item is appended to the end of the note's `body`. If provided, the item is inserted at that 0-based block position; subsequent blocks shift down.
- [ ] Out-of-range `position` (negative or > current `body.size`) returns 400 with a clear error.
- [ ] If the note doesn't exist or is owned by a different account, returns 404.
- [ ] After creation, `GET /notes/{id}` returns the item in the right place in `body`, with the same fields.
- [ ] Test: create a note with one prose block; POST an item with no position → assert it appears at index 1. POST another with `position: 0` → assert ordering is `[item2, prose, item1]`.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[01-block-shaped-notes-foundation]] — the schema and `body: List<Block>` shape must exist first.

## Hints (non-binding)

- **Existing pattern to mirror:** auth feature's `02-register-endpoint` for the request/validation/response shape.
- **Server time:** use the same time source as the rest of the server (likely `Clock.System.now()` from kotlinx-datetime or `Instant.now()`). Be consistent with whichever is already in `Models.kt`.
- **Watch out for:** concurrent inserts at the same `position` from two devices. v1 acceptable behavior is "last write wins on the row" — Construction can either serialize via a transaction or accept the race; both are mob-defensible. Document the choice.

## Out of scope for this story

- Editing or deleting an item (see 03, 04).
- The `Touch` semantics on update (see 03).
- Auto-archive (see 06).
