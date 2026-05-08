---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 2
estimate: 30m
blocked-by:
  - "[[02-render-blocks-with-decay-opacity]]"
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [AN] Long-press an item → contextual menu with `Delete`

**Lane:** Android
**PRD section:** [[PRD#Goals]] G1 (CRUD completeness).
**API contract section:** [[api-contract#DELETE /notes/{id}/items/{item_id}]].

## Why

Users need to remove items they typed by mistake or no longer want. Long-press menu is the chosen UX surface (per [[open-questions#Q1]] driver guess; mob may revise).

## Acceptance criteria

- [ ] Long-pressing an item row anywhere outside the checkbox and drag handle opens a contextual menu (popup or bottom sheet — Construction's call within existing patterns) with at least a `Delete` option.
- [ ] Tapping `Delete` calls `DELETE /notes/{id}/items/{item_id}` and removes the item from local state on success.
- [ ] No confirmation dialog in v1 — delete is immediate. (See [[out-of-scope]] — v1 is hard-delete-only; users restore via re-typing if they regret it.)
- [ ] On network failure, an error is shown and the item remains in local state.
- [ ] Test (VM-level): given a list with item X, invoke `onItemDelete(X.id)`, assert DELETE is called and local state has X removed on success.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — items must render before they can be long-pressed.

## Hints (non-binding)

- **Dropdown vs bottom sheet:** look at how other long-press menus exist in mindnote (e.g. on the home screen note tiles) and match.
- **Watch out for:** long-press conflicts with the drag handle in story 06. The drag handle has its own affordance; long-press *outside* the handle invokes this menu.

## Out of scope for this story

- Swipe-to-delete (Q1 alternative; mob can promote it later).
- Undo via snackbar.
- Deleting prose blocks (a different surface — covered by the existing note edit flow if it exists, or out of scope entirely).
