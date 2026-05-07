---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 1
estimate: 30m
blocked-by:
  - "[[01-token-storage-helper]]"
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/1
---

# [Android] The shared `HttpClient` automatically attaches `Authorization: Bearer <token>` when a token exists

**Lane:** Android
**PRD section:** Goal 6 ("sends the token automatically with every request after login").
**API contract section:** §4 (Bearer requirement on existing endpoints).

## Why

Without this, every existing `NotesApi`/`ChatApi` call would have to be modified to plumb the token through. Auto-attachment via the shared client means existing call sites don't change, and login + new screens just work.

## Acceptance criteria

- [ ] When a token is stored (via [[01-token-storage-helper]]), the shared `HttpClient` includes `Authorization: Bearer <token>` on every outgoing request.
- [ ] When no token is stored, the header is NOT present (so login/register can still call exempt endpoints).
- [ ] Token changes (set after login, cleared after logout) take effect on subsequent requests without restarting the app.
- [ ] Test: with MockEngine, set a token → fire a request → assert header present + correct value. Clear the token → fire a request → assert header absent.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]] (needs the token-read API).

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md) — the shared `HttpClient` is built in `core/di/AppModule.kt` with `defaultRequest`, `ContentNegotiation`, etc. The Ktor mechanism for "always-add-this-header-if-available" is typically a `defaultRequest { ... }` block reading from the token store.

- **Likely files affected:** `core/di/AppModule.kt` (the `HttpClient` builder block).
- **Existing pattern to mirror:** the existing `defaultRequest { header(...) }` in the same file.
- **Watch out for:** the Koin `single { HttpClient(...) }` is built once at app start. If the token reads from a Flow, the `defaultRequest` block must read the *current* value at request time, not capture a snapshot at client-construction time.

## Out of scope for this story

- Handling 401 responses (that's [[08-401-interceptor]]).
- Refresh-token logic (per Decision D6, no refresh tokens).
