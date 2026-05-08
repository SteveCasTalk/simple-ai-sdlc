---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 2
estimate: 45m
blocked-by:
  - "[[02-render-blocks-with-decay-opacity]]"
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [AN] Tap checkbox toggles `done`; item refreshes to full opacity

**Lane:** Android
**PRD section:** [[PRD#Story 3 — Touch resets the timer]], [[PRD#Goals]] G3.
**API contract section:** [[api-contract#PATCH /notes/{id}/items/{item_id}]].

## Why

The most frequent interaction with a checklist is checking items off. Wiring this end-to-end (UI tap → PATCH → reflect server state) also exercises the full `Touch` machinery.

## Acceptance criteria

- [ ] In note detail, tapping an item's checkbox calls `PATCH /notes/{id}/items/{item_id}` with `{ "done": <new state> }`.
- [ ] On a successful response, the local view updates: checkbox flips, opacity returns to 100% (because the response carries the fresh `lastTouchedAt`).
- [ ] On a network failure, the checkbox does *not* flip; show a transient error (snackbar / inline) — exact treatment is Construction's call.
- [ ] An archived item does not render (story 02), so this story does not need to handle "tap on archived" — the user will rescue it via story 08 first.
- [ ] Test (unit, VM-level): given a stub repo that PATCHes successfully, when `onItemDoneToggle(itemId, true)` is invoked, the resulting state has the item with `done = true`, `lastTouchedAt = response time`, opacity = 1.0.
- [ ] Test (unit): given a stub repo that fails, the item stays in its previous state and an error event is emitted.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — items must render with checkboxes before they can be tapped.

## Hints (non-binding)

- **Likely files affected:** the note detail VM, the existing `ChecklistItemRow` composable from story 02 (now with a click handler), and the API client (existing `NotesApi.kt` pattern in `data/remote/`).
- **Optimistic vs server-confirmed update:** v1 is fine with server-confirmed (wait for response, then update state). Optimistic-with-rollback is a polish item.

## Out of scope for this story

- Editing item text (story 04).
- Reordering (story 06).
- Bulk toggle.
