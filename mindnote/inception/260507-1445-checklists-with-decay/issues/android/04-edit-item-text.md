---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 2
estimate: 60m
blocked-by:
  - "[[02-render-blocks-with-decay-opacity]]"
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [AN] Edit item text in note edit mode; refreshes opacity on save

**Lane:** Android
**PRD section:** [[PRD#Story 3 — Touch resets the timer]], [[PRD#Goals]] G3.
**API contract section:** [[api-contract#PATCH /notes/{id}/items/{item_id}]].

## Why

Items are not just toggles — users will edit the text ("buy milk" → "buy oat milk and bread"). Editing must `Touch` the item server-side so the decay clock resets.

## Acceptance criteria

- [ ] In note edit mode, tapping an item's text region puts the user into a text-edit affordance for that single item (cursor in the item's text field). Item-level edit, not whole-body edit.
- [ ] Confirming the edit (existing edit-mode confirm flow — done button / back / focus loss, whichever the existing editor uses) calls `PATCH /notes/{id}/items/{item_id}` with `{ "text": "<new text>" }`.
- [ ] On success: the row updates with new text and opacity = 1.0 (server's response carries fresh `lastTouchedAt`).
- [ ] If the user enters and exits edit mode without changing the text, no PATCH is fired (minor optimization; prevents needless touches).
- [ ] On network failure, the local edit is preserved in the editor for retry; no item state is mutated until the server confirms.
- [ ] Test (VM-level): given an item with text "old", invoke `onItemTextEdit(itemId, "new")`, assert PATCH is called with `{ "text": "new" }` and the resulting state reflects the response.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — items must render before they can be edited.

## Hints (non-binding)

- **Likely files affected:** the note edit VM + composable. Mindnote's existing editor probably treats `body` as a single TextField; with blocks, the editor needs per-block focus/edit state. Construction picks the smallest viable refactor — possibly per-item `TextField`s in a `LazyColumn`.
- **Watch out for:** keyboard handling between items. Tab / next-item navigation is a polish item; not in scope for this story.

## Out of scope for this story

- Creating new items (story 05).
- Reordering (story 06).
- Deleting (story 07).
- Markdown formatting *within* item text — items are plain text only in v1.
