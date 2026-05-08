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

# [BE] Daily cron archives items 60+ days untouched

**Lane:** Backend
**PRD section:** [[PRD#Story 4 — Auto-archive forces a rescue moment]], [[PRD#Goals]] G4.
**API contract section:** [[api-contract#Server-internal: the auto-archive cron]] (no public surface — internal job).

## Why

The whole "force confrontation with stale work" mechanism only works if items actually transition to `archived_at != null` on a schedule. Without this story, items decay forever in opacity but never archive — the rescue flow never gets a chance to surface them.

## Acceptance criteria

- [ ] A scheduled job runs daily at 03:00 UTC.
- [ ] On each run, it sets `archived_at = now` on every item where `archived_at IS NULL` AND `(now − last_touched_at) >= 60 days`. **Both done and undone items are eligible** per [[decisions#D6]].
- [ ] The job is idempotent — running it twice in succession does not change items already archived (their `archived_at` stays at the original value, not refreshed).
- [ ] The job survives mid-run failure: if the process is killed mid-batch, the next run picks up. (Either by transactional batches or by virtue of the simple `UPDATE … WHERE` being one statement.)
- [ ] Test: with system clock mocked, create items with `last_touched_at` 59d / 60d / 61d / 100d ago; run the cron; assert only the 60d+ ones gain `archived_at`. Re-run; assert no further changes.
- [ ] Test: a `done = true` item 60d untouched is also archived (D6).
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[02-create-checklist-item-endpoint]] — items must exist with `last_touched_at` populated; the schema work in story 01 added the column.

## Hints (non-binding)

- **Scheduling mechanism:** Construction picks — options include an in-process Kotlin coroutine loop (cheapest, fits Ktor + Railway), Quartz, or Railway-native cron jobs. The `server/CLAUDE.md` says the server is small and flat — a 30-line `ArchiveCron.kt` started from `Application.module()` is probably the simplest path.
- **Time source:** use the same time source as the rest of the server. Tests mock it.
- **Watch out for:** running the cron on every server cold-start (during dev) is actually fine and makes testing easier. Don't over-engineer the "exactly once at 03:00 UTC" guarantee; "approximately daily, idempotent on repeat" is the spec.

## Out of scope for this story

- The `Rescue` endpoint (story 07).
- Notifying the user that items were archived (push notifications are out of scope per [[decisions#D8]]).
- Any per-user tuning of the 60d threshold — fixed in v1.
