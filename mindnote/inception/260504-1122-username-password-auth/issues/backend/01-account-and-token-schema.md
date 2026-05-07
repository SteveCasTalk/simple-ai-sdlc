---
type: issue
feature: username-password-auth
lane: backend
status: ready
wave: 0
estimate: 60m
blocked-by: []
tags:
  - inception/issue
  - lane/backend
  - feature/username-password-auth
  - status/ready
  - wave/0
---

# [BE] `Account` and `AuthToken` tables exist, with a "resolve account from token" query

**Lane:** Backend
**PRD section:** Goals 1, 4 (foundation for both register and middleware).
**API contract section:** n/a (foundation slice — no endpoint).

## Why

All other backend slices (register, login, logout, auth middleware) need these tables and the lookup query. Calling them out as a separate slice keeps the next four BE stories truly parallel — each can grab without untangling foundation work.

## Acceptance criteria

- [ ] A new table for accounts exists with: a globally-unique id, a unique lowercase username (3–30 chars, `[a-z0-9_]`), a password hash column big enough for an Argon2id encoded string (≥256 chars to be safe), and a `created_at` timestamp.
- [ ] A new table for auth tokens exists with: the opaque token string (unique, indexed), an FK to account.id, and a `created_at` timestamp.
- [ ] A function/query exists that, given a token string, returns the matching `Account` or `null` — used by all four downstream stories.
- [ ] Schema changes are applied via the project's standard migration mechanism (Construction reads the existing pattern in `Database.kt`).
- [ ] Test: persisting an account + token, then calling the resolve-from-token query, returns the right account; with a bogus token, returns null.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- nothing — wave 0.

## Hints (non-binding)

> [!tip]
> See [server/CLAUDE.md](server/CLAUDE.md) for stack: Ktor 3.0.3 + Exposed 0.57 + Postgres + HikariCP. Construction inspects `Database.kt`, `Models.kt`, `ChatTables.kt` for the existing table-definition pattern and migration approach.

- **Likely files affected:** `server/src/main/kotlin/com/mindnote/server/` — new file(s) for the auth tables, plus a touch in `Database.kt` if migrations are wired there. May also need a new file for the resolve helper.
- **Existing pattern to mirror:** `ChatTables.kt` for table definitions; `Database.kt` for connection + migration setup.
- **Watch out for:** the existing data wipe (per Decision D5) — the same deploy that adds these tables wipes the existing `users` and `notes` tables. Confirm the migration order with Construction.

## Out of scope for this story

- Any HTTP endpoint (register, login, logout — separate stories).
- The Bearer middleware itself (separate story).
- Password hashing logic (lives in the register/login stories where it's actually used).
