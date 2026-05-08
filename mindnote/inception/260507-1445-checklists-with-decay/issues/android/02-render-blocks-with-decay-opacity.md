---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 1
estimate: 75m
blocked-by:
  - "[[01-block-aware-domain-and-room]]"
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/1
---

# [AN] Note detail renders prose + items with decay opacity

**Lane:** Android
**PRD section:** [[PRD#Story 2 — See at a glance what's alive vs. stale]], [[PRD#Goals]] G2.
**API contract section:** [[api-contract#GET /notes/{id}]] (read-only consumption).

## Why

This is the user's first encounter with the feature: opening a note, seeing prose paragraphs interleaved with checklist items, and noticing that older items look ghosted. Read-only at this stage — toggling, editing, creating, reordering, deleting all live in subsequent stories.

## Acceptance criteria

- [ ] The note detail screen renders each block in `Note.body` in order:
  - `ProseBlock` → a paragraph of text matching the existing prose styling.
  - `ItemBlock` (where `archivedAt == null`) → a row with a checkbox + text. Checkbox visually reflects `done` state. Tap is **not yet wired** — that's story 03; the checkbox is non-interactive in this slice.
- [ ] Items with `done = true` render at full opacity (1.0) regardless of `lastTouchedAt`.
- [ ] Items with `done = false` render at opacity `max(0.2, 1.0 − (daysSinceLastTouched / 30) × 0.8)`. Verified by a unit test that constructs `ItemBlock`s with `lastTouchedAt` of 0d / 15d / 30d / 50d ago and asserts the computed opacity matches `1.0 / 0.6 / 0.2 / 0.2`.
- [ ] Decay opacity is computed locally from `lastTouchedAt` and the device clock (server is timekeeping authority, not fade-level emitter — per [[decisions#D3]]).
- [ ] Archived items (`archivedAt != null`) are excluded from the rendered body in this slice. (The "Show N archived" expander is story 08.)
- [ ] An existing pre-feature note (whose body migrated to a single `ProseBlock`) renders correctly with no items shown.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[01-block-aware-domain-and-room]] — the domain model must carry blocks before the screen can render them.

## Hints (non-binding)

- **Likely files affected:** the existing note detail screen composable + its ViewModel, plus a new small composable for `ChecklistItemRow`. Look at the existing notes-screen patterns first — the home recents screen probably has the closest layout idiom.
- **Existing pattern to mirror:** the OCR review screen (Scan flow) for "screen-with-list-of-mixed-content" rendering.
- **Opacity formula in code:** consider an extension `ItemBlock.opacityFor(now: Instant): Float` so the math has one home and the test can drive it directly without composing the whole UI.
- **Watch out for:** computing `Instant.now()` inside a Composable is non-deterministic for tests; pass it in via VM state or accept a `Clock`.

## Out of scope for this story

- Tapping the checkbox to toggle done (story 03).
- Editing item text (story 04).
- Creating new items (story 05).
- The "Show N archived" expander (story 08).
- Auto-refreshing opacity *while the note is open* — render-on-open is enough for v1.
