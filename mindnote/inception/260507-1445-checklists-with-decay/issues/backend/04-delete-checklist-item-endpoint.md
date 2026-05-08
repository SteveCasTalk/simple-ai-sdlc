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

# [BE] `DELETE /notes/{id}/items/{item_id}` — hard delete, idempotent

**Lane:** Backend
**PRD section:** [[PRD#Goals]] G1 (CRUD completeness for items).
**API contract section:** [[api-contract#DELETE /notes/{id}/items/{item_id}]].

## Why

Items are user-generated content; users must be able to remove them. Idempotent deletion is friendlier to flaky-network mobile clients (per [[open-questions#Q5]] default).

## Acceptance criteria

- [ ] `DELETE /notes/{id}/items/{item_id}` removes the item from its parent note's `body` and returns 204 No Content.
- [ ] Calling DELETE on an item that no longer exists (or never did) returns 204 (idempotent — never 404).
- [ ] Calling DELETE on a note that doesn't exist still returns 204 if the path is well-formed (idempotency extends to the note layer).
- [ ] DELETE on an archived item also succeeds — archive is a soft state, delete is destructive and bypasses it.
- [ ] After delete, `GET /notes/{id}` returns the note with the item removed; the surrounding blocks shift up to fill the gap.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[02-create-checklist-item-endpoint]] — items must exist to be deleted.

## Hints (non-binding)

- **Idempotency:** if the implementation does a `DELETE` SQL with row-count check, just don't 404 on row count = 0. Treat it as success.
- **Watch out for:** if Construction stored items in a separate `note_blocks` table, deleting a block must update the `position` of subsequent blocks (or use a sparse position scheme — both fine, mob ratifies in review).

## Out of scope for this story

- Soft delete with undo. v1 is hard-delete-only.
- Bulk delete (e.g. "delete all completed").
