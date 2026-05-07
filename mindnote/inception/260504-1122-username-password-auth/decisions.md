---
type: decisions
feature: username-password-auth
status: draft
created: 2026-05-04
tags:
  - inception/decisions
  - feature/username-password-auth
---

# Decisions

> [!info] **For mob ratification.** Driver decisions made during this Inception that the mob should review and either ratify or overturn.

### D1 — Argon2id for password hashing

- **Decision:** Use Argon2id (not bcrypt, not scrypt, not PBKDF2).
- **Rationale:** Modern best-practice; resistant to both side-channel and GPU attacks. Adds one dependency (a JVM Argon2 library — exact lib chosen during Construction; flagged as new-dep proposal at that time).
- **Driver:** confirmed in inception Q5.

### D2 — `409 username_taken` leaks username existence

- **Decision:** `POST /auth/register` returns `409` with code `username_taken` when the username is in use, distinct from `400 validation_failed`.
- **Trade-off:** This leaks whether a username exists in the system. Industry standard is to either (a) accept the leak (most apps do), or (b) merge into a generic "registration failed" (hurts UX significantly).
- **Choice:** (a) — accept the leak. Username enumeration is low-value attack against a notes app with no public profiles, and merging the responses creates a frustrating UX where users can't tell whether to retry with a different username or fix their password.
- **Mob:** review and ratify or push back.

### D3 — `401 invalid_credentials` is non-distinguishing

- **Decision:** `POST /auth/login` returns the same `401 invalid_credentials` whether the username doesn't exist or the password is wrong.
- **Rationale:** Login is the more security-sensitive endpoint than register. Non-distinguishing responses prevent online enumeration via the login form.
- **Mob:** ratify or overturn.

### D4 — Android stores the token in DataStore Preferences (`UserPrefs`)

- **Decision:** Reuse the existing `UserPrefs` (DataStore Preferences) for token storage. Token is plaintext on disk.
- **Trade-off:** EncryptedSharedPreferences (Keystore-backed) is more secure but adds setup. Notes are not financial / health data; threat model = root access or backup theft, both of which compromise other app data anyway.
- **Driver:** confirmed in inception Q13.

### D5 — Wipe existing notes + users on this feature's deploy

- **Decision:** The deploy that ships this feature runs a one-time data wipe of the `users` and `notes` tables (and any FKs).
- **Rationale:** The current users are test accounts, the current notes are test data. No user value lost; saves writing migration logic.
- **Driver:** confirmed in inception Q10.
- **Operations note:** the wipe is part of the deploy procedure for this feature and is safe to run repeatedly until first real users register.

### D6 — Long-lived tokens, single-token revocation on logout

- **Decision:** Tokens never expire by time. Logout revokes only the *current* token (the one in the request header), not all tokens for the account.
- **Rationale:** Simple model that maps to "log out from this device only" — the most common user expectation. "Log out everywhere" is a future feature.
- **Driver:** confirmed in inception Q9 (option A).

### D7 — Single template for all lanes (carryover from framework v0.2)

- **Decision:** Story files use one shape across BE + Android (lane is just a tag). No per-lane "implementation steps" or "files to touch" sections — Construction owns those.
- **Rationale:** Carryover of the AI-SDLC framework's Inception v0.2 boundary change. Documented for traceability since this is the first feature spec'd under v0.2.

### D8 — H2 in-memory for BE DB-backed unit tests (logged in Construction)

- **Decision:** Add `com.h2database:h2:2.3.232` as a `testImplementation` in `server/build.gradle.kts` and write DB-backed unit tests against an in-memory H2 instance (PostgreSQL compatibility mode).
- **Rationale:** The BE auth foundation story (#23) needs a test that persists `Account`+`AuthToken` then resolves an Account from a token — a real DB. Existing BE tests (`OcrRoutesTest`) are pure-HTTP and don't touch the DB, so there's no precedent. H2 in-memory is the lightest path; Testcontainers-Postgres would be heavier (Docker dependency in CI).
- **Trade-off:** H2 is not Postgres — features like `RETURNING`, jsonb, partial indexes won't behave identically. For the auth slice (basic tables, FKs, unique indexes, simple SELECT/JOIN) this is fine. If a future story needs Postgres-only behavior the test for that story switches to Testcontainers.
- **Driver:** confirmed in Construction of #23.
