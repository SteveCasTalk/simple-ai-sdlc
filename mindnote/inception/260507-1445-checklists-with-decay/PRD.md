---
type: prd
feature: checklists-with-decay
status: draft
created: 2026-05-07
tags:
  - inception/prd
  - feature/checklists-with-decay
  - status/draft
---

# PRD: Checklists with decay

> [!info] **Status:** Draft / awaiting mob review · **Driver:** NguyenKhacPhuc · **Last updated:** 2026-05-07
> See [[_index]] for the parallel-work plan and [[open-questions]] for unresolved items.

## One-line intent

Add **first-class checklist items** to mindnote notes (mixed inline with prose paragraphs), with a **decay UX** where untouched items visually fade over 30 days and auto-archive at 60 days behind a "rescue" button — making stale work confront the user instead of hiding silently.

## Problem

Today, a mindnote note's `body` is plain text. Users can't track to-do-style items inside a note in a way that distinguishes "I'm going to do this" from "I wrote this down once and will never touch it again." Existing voice-of-discipline tools (basic todo apps, markdown checkboxes) have no honesty mechanism — items accumulate forever and the list becomes a museum of past intentions ("LARP").

The cost: users lose trust in their own checklists. Lists become noise rather than signal. The driver wants the *visual weight* of an item to mirror *how recently the user actually engaged with it*, so a glance at a list reveals what's alive vs. what's wishful.

## Goals

Testable. Each gets at least one acceptance criterion in a story.

- [ ] **G1.** A note can contain checklist items mixed inline with prose paragraphs (Y-prose-mixed). User can convert any line into an item via markdown shortcut (`- [ ] `) or a toolbar button.
- [ ] **G2.** Each checklist item has an opacity that decays linearly from 100% at `last_touched_at` to 20% over 30 days, then stays at 20% until day 60. (Done items do not fade.)
- [ ] **G3.** An item is "touched" — and `last_touched_at` resets to now — when the user toggles its done state, edits its text, or reorders it. Opening or scrolling past a note does **not** count as a touch.
- [ ] **G4.** At 60 days untouched, an item auto-archives: removed from the live list, hidden behind a `▶ Show N archived` expander at the bottom of its parent note. Done items at 60d also archive.
- [ ] **G5.** Tapping `Rescue` on an archived item resets `last_touched_at = now`, returns it to the top of the live list at full opacity, and unsets `archived_at`.
- [ ] **G6.** Decay timestamps and the auto-archive cycle are server-authoritative. Client renders fade locally from server-supplied `last_touched_at` (no per-scroll round trips).

## Non-goals

What this feature is explicitly **not** doing in v1:

- **Push notifications** about expiring or archived items. (See `out-of-scope.md`.)
- **Nesting / sub-items.** Flat lists only.
- **Per-item "pin / never decay" opt-out.** Workaround: toggle the item's done state to refresh `last_touched_at`.
- **Bulk operations** (rescue-all, archive-all, export).
- **Tags or due dates on items.** Notes already have tags; that covers the category need.
- **iOS client.** Project has no iOS lane today.

## User stories

### Story 1 — Capture a thought-then-task as you type

**As a** user jotting down a note, **I want** to convert any line into a checklist item by typing `- [ ] ` at line start (or hitting a toolbar button), **so that** I can mix narrative reflection and concrete actions in the same note without switching apps or note-types.

**Acceptance criteria:**
- [ ] Typing `- [ ] ` at the start of a line in the note editor converts that line into a checkbox-prefixed item, with the cursor placed after the box ready for typing.
- [ ] A toolbar button "Add checklist item" inserts a new empty item at the current cursor position (creates a new line if mid-prose).
- [ ] On save, the item is persisted with `last_touched_at = now`, `done = false`, `archived_at = null`.

### Story 2 — See at a glance what's alive vs. stale

**As a** user opening a note with old items, **I want** older items to look ghosted and recent items to look crisp, **so that** my attention lands on what I actually still care about.

**Acceptance criteria:**
- [ ] An item's rendered opacity = `max(0.2, 1.0 − (days_since_last_touch / 30) × 0.8)` — so day 0 = 100%, day 15 = 60%, day 30+ = 20%.
- [ ] Items with `done = true` always render at 100% opacity regardless of `last_touched_at`.
- [ ] The opacity computation uses the server-supplied `last_touched_at` and the *device clock* — no extra server round trips per scroll.

### Story 3 — Touch resets the timer

**As a** user re-engaging with a stale item, **I want** any meaningful interaction (toggle, edit, reorder) to refresh the item, **so that** the visual state stays honest about what I'm actually working on.

**Acceptance criteria:**
- [ ] Toggling an item's done state resets `last_touched_at = now`.
- [ ] Editing an item's text (any change to the text field) resets `last_touched_at = now` on save.
- [ ] Reordering an item via drag handle resets `last_touched_at = now` for that item only.
- [ ] Opening the note, scrolling past the item, or editing a *different* item does **not** reset `last_touched_at`.

### Story 4 — Auto-archive forces a rescue moment

**As a** user with items 60+ days untouched, **I want** them removed from the live list and stashed behind a single click, **so that** I confront stale work explicitly rather than skimming over it forever.

**Acceptance criteria:**
- [ ] A daily server cron (03:00 UTC) sets `archived_at = now` on every item where `(now − last_touched_at) ≥ 60 days` and `archived_at IS NULL`. Both done and undone items are archived.
- [ ] In a note with archived items, the live list ends with `▶ Show N archived` (where N is the archived count). Tapping expands the archived items inline below the live list.
- [ ] An archived item displays a `Rescue` button. Tapping it calls the rescue endpoint, which: sets `archived_at = NULL`, sets `last_touched_at = now`, and returns the item to the top of the live list at 100% opacity.

### Story 5 — Items roundtrip correctly through the API

**As a** mobile client, **I want** to fetch a note and receive its blocks (prose + items) in a single response, with `last_touched_at` and `archived_at` populated, **so that** I can render the decay state without secondary calls.

**Acceptance criteria:**
- [ ] `GET /notes/{id}` returns `body: List<Block>` where each block has `type: "prose" | "item"`. Item blocks include `id`, `text`, `done`, `last_touched_at`, `archived_at`.
- [ ] By default, `archived_at != null` items are excluded from the response. Response also includes a sibling `archived_count: int` so the client knows whether to show the expander.
- [ ] `GET /notes/{id}?include_archived=true` returns archived items inline (with their original positions preserved), each with `archived_at` populated.

## Success metrics

How we know this worked, after launch. The driver picks targets the mob can refine.

- **M1 — Items per active user / week.** Target: median ≥ 3 items created per active user per week within 30 days of launch. Measures whether the feature is reachable / discoverable.
- **M2 — Stale-item ratio.** Target: items with opacity < 0.3 should be ≤ 30% of the live list (median user) by day 60 of usage. Measures whether decay is *working* — i.e., users are either touching items (resetting decay) or letting them archive (driving the ratio down). A persistently high ratio means the rescue/archive flow isn't engaging users.
- **M3 — Rescue rate.** Of items presented in the archived expander, what fraction get rescued? Target: 5–25%. Below 5% suggests users never open the expander; above 25% suggests decay is too aggressive (users don't agree with the auto-archive verdict).

## Constraints

- **No deadline.** Ship when it ships.
- **No external integrations** (calendar / reminders / push notifications).
- **Server is timekeeping authority** — `last_touched_at` set server-side on every mutating call. Client never sends `last_touched_at`.
- **Single-user-but-multi-device** — mindnote already supports multi-device per account. Conflict resolution is last-write-wins on `last_touched_at` (the latest mutation timestamp from any device wins).
- **Migration of existing notes.** Existing notes' `body` is a plain string today. Policy: convert each existing note to `body = [{type: "prose", text: <old body>}]`. Do **not** wipe (unlike the auth feature's account migration).

## Links

- API contract: `./api-contract.md`
- Project context: `../../CONTEXT.md`
- Issues: `./issues/`
- Decisions: `./decisions.md`
- Out of scope: `./out-of-scope.md`
- Open questions: `./open-questions.md`
