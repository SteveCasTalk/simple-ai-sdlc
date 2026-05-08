---
type: issue
feature: checklists-with-decay
lane: backend
status: ready
wave: 2
estimate: 60m
blocked-by:
  - "[[02-create-checklist-item-endpoint]]"
tags:
  - inception/issue
  - lane/backend
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [BE] `PATCH /notes/{id}/items/{item_id}` — update text/done/position; resets `last_touched_at`

**Lane:** Backend
**PRD section:** [[PRD#Story 3 — Touch resets the timer]], [[PRD#Goals]] G3.
**API contract section:** [[api-contract#PATCH /notes/{id}/items/{item_id}]].

## Why

This endpoint is where `Touch` semantics become real on the server. Every successful PATCH resets `last_touched_at = now`, which is the input that drives client-side opacity decay. Without this, decay is a one-way slide — there's no way for the user to keep an item "alive."

## Acceptance criteria

- [ ] `PATCH /notes/{id}/items/{item_id}` accepts a partial body — any subset of `{ text, done, position }` — and applies it. Returns 200 with the full updated item block.
- [ ] **Touch:** every successful PATCH sets the item's `last_touched_at = now`, *even if the patched fields would result in the same stored values* (e.g. `{ "done": false }` when `done` is already `false`). Verified by an integration test that PATCHes with no-op data and asserts `last_touched_at` advanced.
- [ ] PATCH on an archived item (`archived_at != null`) returns 409 Conflict with body `{ "error": { "code": "item_archived", "message": "..." } }` per [[open-questions#Q4]] default.
- [ ] PATCH on a non-existent item returns 404.
- [ ] Editing *prose blocks in the same note* via `PATCH /notes/{id}` (story 01's body-replace path) does NOT touch any item that rode along unchanged. Asserted by a test: GET item's `last_touched_at`, PATCH note body with the same item entry intact, GET again, assert timestamp unchanged.
- [ ] `position` change reorders the item within `body` and reshuffles other blocks accordingly. Out-of-range position returns 400.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[02-create-checklist-item-endpoint]] — items must exist before they can be patched.

## Hints (non-binding)

- **Touch semantics centralization:** consider one helper `touchItem(itemId)` called at the end of every successful mutation path so the rule is enforceable in one place. Construction picks the shape.
- **Watch out for:** the "PATCH note body with item ridealong" test is easy to get wrong. The rule is *"compare incoming item field-by-field against stored; if anything differs, touch; otherwise leave."* Don't accidentally touch every item that appears in the array.

## Out of scope for this story

- The `Rescue` flow for archived items (story 07 — separate endpoint).
- Auto-archive cron (story 06).
- Hard delete (story 04).
