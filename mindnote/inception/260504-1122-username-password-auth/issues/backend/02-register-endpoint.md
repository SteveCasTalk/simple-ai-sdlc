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

# [BE] `POST /auth/register` creates an account, hashes the password, returns a token

**Lane:** Backend
**PRD section:** Goal 1, Story 1.
**API contract section:** §1 `POST /auth/register`.

## Why

The first endpoint a new visitor calls. Combines schema (already there from BE-1), Argon2id hashing (new dependency), and the same token-issuance shape login also returns.

## Acceptance criteria

- [ ] Sending a valid `{username, password}` returns `201` with `{token, account: {id, username}}` and persists both the account (with Argon2id-hashed password) and the token.
- [ ] Sending a username that violates the format rules (too short, has uppercase, has spaces, has special chars other than `_`) returns `400 validation_failed` with a `fields.username` message — no account created.
- [ ] Sending a password that violates any of the five rules (length ≥ 8, ≥1 upper / lower / digit / special) returns `400 validation_failed` with a `fields.password` message naming the violated rule(s) — no account created.
- [ ] Sending a username that already exists (case-insensitive — usernames are stored lowercase) returns `409 username_taken` (per Decision D2) — no second account created.
- [ ] Server logs do not contain the raw password anywhere (grep for the test password in log output → 0 matches).
- [ ] Test: end-to-end against the real DB (or a test DB) — register succeeds, second register with same username returns 409, register with weak password returns 400.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- [[01-account-and-token-schema]]

## Hints (non-binding)

> [!tip]
> Argon2id needs a JVM library — not currently in `server/build.gradle.kts`. Construction must propose a specific library to the driver (per Construction skill rule on new deps), get a yes, log it in [decisions](decisions.md), and add as a separate `chore(server): add <argon2-lib>` commit before the impl commits.

- **Likely files affected:** new `AuthRoutes.kt` (or extend `Routes.kt`); a small `Passwords.kt` for the hash/verify helpers; `Application.kt` to mount the new route group.
- **Existing pattern to mirror:** `ChatRoutes.kt` for route extension functions; `Dto.kt` for request/response shapes.
- **Watch out for:** the response shape MUST match the API contract exactly — Android stories test against this contract via MockEngine.

## Out of scope for this story

- The Bearer middleware (separate story BE-5).
- Login endpoint (separate story BE-3).
- Logging in immediately after register from the *client* (Android stories — server side just returns the token).
