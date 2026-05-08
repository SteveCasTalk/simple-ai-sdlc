---
type: issue
feature: checklists-with-decay
lane: backend
status: ready
wave: 2
estimate: 45m
blocked-by:
  - "[[02-create-checklist-item-endpoint]]"
tags:
  - inception/issue
  - lane/backend
  - feature/checklists-with-decay
  - status/ready
  - wave/2
---

# [BE] `GET /notes` exposes `archived_count` and `?include_archived` query param

**Lane:** Backend
**PRD section:** [[PRD#Story 4 — Auto-archive forces a rescue moment]], [[PRD#Story 5 — Items roundtrip correctly through the API]].
**API contract section:** [[api-contract#GET /notes/{id}]].

## Why

The Android `▶ Show N archived` expander needs two things from the server: a count (always returned) and a way to fetch the actual archived items on demand. This story decouples archival *visibility* from archival *creation* (the cron in story 06) so both BE and Android can build/test against fixtures.

## Acceptance criteria

- [ ] `GET /notes/{id}` (default — `include_archived` absent or false): response excludes any item where `archived_at != null` from `body`. The response always includes `archived_count: int` representing the count of archived items in this note.
- [ ] `GET /notes/{id}?include_archived=true`: response includes archived items inline at their stored position in `body`, with `archived_at` populated. `archived_count` still reflects the total.
- [ ] `GET /notes` (list endpoint): each note carries `archived_count`. Default behavior is exclude-archived (consistent with the detail endpoint).
- [ ] Test fixture: a note with 3 active items + 2 archived items (where `archived_at` is manually set in the test setup). Default GET returns 3 items + `archived_count: 2`. With `?include_archived=true`, returns all 5 with `archived_at` populated for the 2.
- [ ] Performance sanity: `archived_count` should be cheap (one COUNT query per note, or a precomputed column). Construction picks. **Don't issue a separate query per note in the list endpoint.**
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[02-create-checklist-item-endpoint]] — items must be creatable, and the schema must include `archived_at` (which it does as of story 01's foundation).

## Hints (non-binding)

- **Test setup for archived fixtures:** since the cron (story 06) is the only legitimate way to set `archived_at`, your tests will need a way to set it directly — either via a test-only repository hook or by manipulating the DB in a `@BeforeEach` block. Construction picks.
- **Watch out for:** the list endpoint's `archived_count` per note is the easy place to N+1 yourself. Consider one batched query.

## Out of scope for this story

- The cron that actually populates `archived_at` (story 06).
- The `Rescue` flow (story 07).
