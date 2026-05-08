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

# [AN] Drag-handle to reorder items; reorder counts as a `Touch`

**Lane:** Android
**PRD section:** [[PRD#Story 3 — Touch resets the timer]], [[PRD#Goals]] G3.
**API contract section:** [[api-contract#PATCH /notes/{id}/items/{item_id}]] (with `position`).

## Why

Lists need to be reorderable. Reordering also counts as a `Touch` per [[decisions#D5]] — the act of repositioning is engagement.

## Acceptance criteria

- [ ] Each item row in note detail (or note edit mode — UX placement is Construction's call) has a visible drag handle.
- [ ] Long-press-and-drag on the handle initiates a reorder gesture; the user can drop the item at a new position within the live items.
- [ ] On drop, the client calls `PATCH /notes/{id}/items/{item_id}` with `{ "position": <new index> }`.
- [ ] Reordering is constrained to within the *live* items section — items cannot be dragged into the archived expander or interleaved with prose blocks via this gesture. (Mixing items with prose is an editor-level capability; not gestural reorder.)
- [ ] On success: the item's `lastTouchedAt` is now fresh (per `Touch` semantics), so its opacity returns to 100% if it had decayed. Local order matches server response.
- [ ] On failure: the visual order reverts to the pre-drag state.
- [ ] Test (VM-level): given a list of items A/B/C, invoke `onItemMove(B.id, position=0)`, assert the PATCH is called with `{ "position": 0 }` and the local state reorders correctly on success.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — items must render before they can be dragged.

## Hints (non-binding)

- **Compose drag-and-drop:** Compose's official drag/drop primitives have evolved; check the version pinned in `gradle/libs.versions.toml` before importing. There may be community patterns the project already uses elsewhere — search the codebase first.
- **Position semantics:** the API takes a 0-based block-list position (across all blocks, prose included). The drag UI only lets the user drop within live items, so Construction must compute the corresponding block-list position from the item-list position.

## Out of scope for this story

- Drag interleaving items with prose (or moving items between notes).
- Multi-select reorder.
- Keyboard / accessibility reorder. (A v2 polish.)
