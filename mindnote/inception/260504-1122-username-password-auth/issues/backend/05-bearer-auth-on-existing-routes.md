---
type: issue
feature: username-password-auth
lane: backend
status: ready
wave: 1
estimate: 60m
blocked-by:
  - "[[01-account-and-token-schema]]"
tags:
  - inception/issue
  - lane/backend
  - feature/username-password-auth
  - status/ready
  - wave/1
---

# [BE] All existing endpoints require a valid Bearer token, scoped to the resolved account

**Lane:** Backend
**PRD section:** Goals 4, 5; Story 5 (note isolation by account).
**API contract section:** §4 (Bearer requirement on existing endpoints).

## Why

Without this, register/login/logout are decorative — the rest of the API is still wide open. This story is the actual security boundary. It also makes notes per-account: handlers can read the resolved `Account` and scope queries by `account_id`.

## Acceptance criteria

- [ ] Every route currently in `Routes.kt` and `ChatRoutes.kt` (notes, favorites, chat, etc.) returns `401 unauthenticated` when called without `Authorization: Bearer <token>`.
- [ ] The same routes return `401 invalid_token` when called with a Bearer token that doesn't exist (or was revoked) in the auth tokens table.
- [ ] With a valid token, the routes work as before, AND the resolved `Account` is available to the handler (e.g. via a Ktor call attribute or extension property like `call.account`).
- [ ] At least one existing notes endpoint is updated to scope by `account_id` (Construction picks which one — typically `GET /notes` is the most observable). Two accounts created in the same test see disjoint notes lists.
- [ ] **Exempt** routes still work without auth: `POST /auth/register`, `POST /auth/login`, `GET /health`.
- [ ] Test: e2e — register account A, post a note as A, register account B, list notes as B → empty; list notes as A → A's note. Without any token, both endpoints → 401.
- [ ] Lane verify passes.

## Blocked by

- [[01-account-and-token-schema]]

## Hints (non-binding)

> [!tip]
> Ktor has built-in `Authentication` plugin support (see ktor-server-auth in deps — note: not currently in `server/build.gradle.kts`, so this may need a new-dep proposal to the driver). Alternative: a hand-rolled route filter that reads the header and calls the resolve-from-token query from BE-1. Construction picks based on trade-off + driver confirmation.

- **Likely files affected:** `Application.kt` (install the auth plugin or wire the filter); `Routes.kt` and `ChatRoutes.kt` (wrap existing routes; one updated to scope by account_id); a new file for the auth principal / filter.
- **Existing pattern to mirror:** how `StatusPages` and `CallLogging` are installed in `Application.kt`.
- **Watch out for:** the exempt list — register / login / health must NOT require auth, or the system can never bootstrap. Get this wrong and CI passes locally but production is locked out.

## Out of scope for this story

- Updating *every* notes endpoint to scope by account — pick one for the story (the next data slice can sweep the rest, OR Construction may complete all of them in this story if cheap). Driver judgment.
- Migrating existing data (per Decision D5, existing `users` and `notes` rows are wiped — no migration logic needed).
