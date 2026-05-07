---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 0
estimate: 30m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/0
---

# [Android] Token can be saved, read, and cleared in `UserPrefs` and survives app restart

**Lane:** Android
**PRD section:** Goal 6, Story 4 (auth survives app restart).
**API contract section:** n/a (client-side state).

## Why

Every other Android slice (HTTP attach, login, logout, app entry) reads or writes the auth token through this helper. Carving it out as a wave-0 slice lets login/logout/etc. proceed without untangling persistence.

## Acceptance criteria

- [ ] A helper exists with three operations: save token, read token (returns null if not set), clear token.
- [ ] Saving then re-reading returns the same string. Clearing then re-reading returns null.
- [ ] State survives process death — verified via instrumentation test or a unit test that re-instantiates the underlying store.
- [ ] Per Decision D4, storage is the existing `UserPrefs` (DataStore Preferences) — plaintext on disk is acceptable.
- [ ] Test: round-trip + clear; the persistence is observable across a fresh `UserPrefs` instance pointing at the same context.
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- nothing — wave 0.

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md) — DataStore Preferences is the persistence layer; `UserPrefs` already exists at `app/src/main/java/com/mindnote/core/storage/UserPrefs.kt`.

- **Likely files affected:** `UserPrefs.kt` (extend with auth-token key + getter/setter/clearer). Possibly a tiny test class.
- **Existing pattern to mirror:** the existing keys/methods in `UserPrefs.kt`.
- **Watch out for:** DataStore Preferences APIs are async (Flow / suspend) — the helper should expose suspending or Flow APIs, not blocking ones.

## Out of scope for this story

- Encrypted storage (per Decision D4).
- Multi-account token storage (only one active token at a time on a device — multi-device is server-side via multiple AuthToken rows).
