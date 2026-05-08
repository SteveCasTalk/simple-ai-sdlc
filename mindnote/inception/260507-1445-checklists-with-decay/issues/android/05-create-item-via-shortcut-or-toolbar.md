---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 2
estimate: 75m
blocked-by:
  - "[[02-render-blocks-with-decay-opacity]]"
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [AN] Create a new checklist item — markdown shortcut or toolbar button

**Lane:** Android
**PRD section:** [[PRD#Story 1 — Capture a thought-then-task as you type]], [[PRD#Goals]] G1.
**API contract section:** [[api-contract#POST /notes/{id}/items]].

## Why

Items don't exist until the user can put one into a note. Two paths to the same outcome (one user-observable behavior — a new item appears):

1. **Markdown shortcut.** Typing `- [ ] ` at the start of a line in the editor.
2. **Toolbar button.** Tapping a "Add checklist item" button in the editor toolbar.

Both result in a newly-created item at the cursor position with empty text and the cursor in the new item ready to type.

## Acceptance criteria

- [ ] In note edit mode, typing the literal characters `- [ ] ` (dash, space, open-bracket, space, close-bracket, space) at the **start of a line** converts that line into a new empty item block at that position. The dash/bracket characters are *not* preserved as text — they are the trigger and disappear.
- [ ] In note edit mode, an "Add checklist item" toolbar button (or an equivalent action — exact UX surface is Construction's call within the existing editor toolbar conventions) inserts a new empty item block at the current cursor position. If the cursor is mid-prose, the new item appears on a new line below the current cursor.
- [ ] Both paths call `POST /notes/{id}/items` with the appropriate `text` (initial text — empty string is acceptable; or the text the user immediately types, if Construction prefers to debounce).
- [ ] On success, the response item is added to the local body state at the correct position and the editor focus moves to the new item's text field.
- [ ] On network failure, an error is shown and the local body remains unchanged (no orphan placeholder items).
- [ ] Test: simulating typing `- [ ] ` at line start results in a `POST /notes/{id}/items` call and a new item state. Simulating a toolbar tap results in the same. Both leave focus on the new item.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — items must be renderable before creation makes sense.

## Hints (non-binding)

- **Markdown trigger detection:** debounce the "starts with `- [ ] `" check on each keystroke. Don't run a full markdown parser for one trigger.
- **Empty-text item:** acceptable for v1. The user is about to type. If they navigate away with empty text, you have an empty item; v1 leaves it (decay will eventually archive it; or the user deletes it via story 07). A v2 polish is "auto-discard empty item on focus-loss."
- **Watch out for:** the cursor position on a brand-new item is finicky in Compose. Test by automating a sequence: focus the editor, simulate the typed input, assert focus.

## Out of scope for this story

- Reordering (story 06).
- Editing existing item text (story 04).
- Multi-line item text — items are single-line in v1; pressing Enter inside an item text field creates a *new* item, not a line break.
