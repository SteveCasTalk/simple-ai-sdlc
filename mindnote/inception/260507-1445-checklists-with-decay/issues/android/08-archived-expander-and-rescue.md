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

# [AN] `▶ Show N archived` expander + per-item Rescue button

**Lane:** Android
**PRD section:** [[PRD#Story 4 — Auto-archive forces a rescue moment]], [[PRD#Goals]] G4, G5.
**API contract section:** [[api-contract#GET /notes/{id}]] (with `?include_archived=true`), [[api-contract#POST /notes/{id}/items/{item_id}/rescue]].

## Why

The whole point of decay is the moment-of-truth when stale items get archived and the user has to decide: rescue (re-engage) or let them go. This story is that moment-of-truth UI.

## Acceptance criteria

- [ ] When the rendered note has `archived_count > 0`, a `▶ Show N archived` expander row appears at the bottom of the live items section (above any trailing prose, if rendering allows that — otherwise simply at the very bottom of the note).
- [ ] Tapping the expander triggers a refetch of the note with `?include_archived=true` and inline-displays the archived items below the expander, with the indicator flipped to `▼ Hide N archived`.
- [ ] Each archived item renders distinctly from live items: dimmed beyond the live opacity floor (e.g. 0.3 with a strikethrough text style — Construction picks the visual treatment to clearly signal "archived"), and shows a `Rescue` button.
- [ ] Tapping `Rescue` calls `POST /notes/{id}/items/{item_id}/rescue`. On success, the item disappears from the archived section and re-appears at the top of the live list at full opacity. `archived_count` decrements; if it drops to 0, the expander disappears.
- [ ] No confirmation dialog before rescue (per [[out-of-scope]] — rescue is non-destructive).
- [ ] Test (VM-level): given a note with 2 archived items, when the expander is tapped, the include-archived fetch is dispatched. When `onRescue(itemId)` is invoked, the rescue endpoint is called and the item moves from archived to live state on success.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- [[02-render-blocks-with-decay-opacity]] — block rendering must exist; this story adds the archived-section variant.

## Hints (non-binding)

- **Two-state fetch:** the simplest model is a VM flag `includeArchivedExpanded` that, when true, refetches the note with the query param. Alternatively, fetch all blocks once on initial load (with `?include_archived=true`) and just visibility-toggle locally — saves one round trip but always pays for the bigger payload. Construction picks based on the typical archived-count.
- **Visual styling:** mob will probably want the archived-item style refined in design review. Ship a defensible default (dimmed + strikethrough or muted color); accept the design feedback in a polish PR.

## Out of scope for this story

- Rescue-all bulk action.
- Search-includes-archived (out of scope — see [[out-of-scope]]).
- Showing the cron's most-recent-archive timestamp ("Last archived: 03:00 UTC").
