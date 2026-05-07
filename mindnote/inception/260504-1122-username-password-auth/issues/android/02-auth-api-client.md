---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 0
estimate: 45m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/0
---

# [Android] An API client can call register / login / logout against the contract

**Lane:** Android
**PRD section:** Goals 1, 2, 3, Stories 1, 2, 3.
**API contract section:** §1, §2, §3 (`/auth/register`, `/auth/login`, `/auth/logout`).

## Why

A typed wrapper over the three auth endpoints, used by the login screen, register screen, and logout flow. Carving it out lets the UI stories grab in wave 2 without re-litigating networking.

## Acceptance criteria

- [ ] A class exists exposing three suspending operations: `register(username, password)`, `login(username, password)`, `logout()`.
- [ ] On a successful register or login, the operation returns a typed result containing the token + the account info from the response body.
- [ ] On a 4xx, the operation surfaces the server's error code (`validation_failed`, `username_taken`, `invalid_credentials`, `unauthenticated`, `invalid_token`) as a typed result the caller can match — not as an exception that loses the code.
- [ ] On a network failure (no connectivity, server unreachable), the operation surfaces a distinguishable "network error" result.
- [ ] Tests use the lane's standard mock-HTTP approach (Construction picks; for Ktor that's typically `MockEngine`) to assert: happy register, happy login, 409 register, 401 login, 204 logout — all map to the right typed results.
- [ ] Lane verify passes.

## Blocked by

- nothing — wave 0. (This story can be done entirely against the [api-contract](../../api-contract.md) without the BE endpoints existing; tests use a mock engine.)

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md) — Ktor 3.0.3 + kotlinx.serialization. The shared `HttpClient` in `core/di/AppModule.kt` is reused (do NOT construct a new client). Existing pattern: `NotesApi(private val client: HttpClient)`. Logout uses `Authorization: Bearer <token>` — the bearer attach mechanism comes from [[03-http-client-attaches-bearer]] but for *this* story, logout can take the token as an explicit method arg so it doesn't depend on A-3.

- **Likely files affected:** new file under `app/src/main/java/com/mindnote/data/remote/`; matching test under `app/src/test/...`. Koin registration in `core/di/AppModule.kt`.
- **Existing pattern to mirror:** `NotesApi.kt` for class shape; whatever DTO/error pattern exists in `Dto.kt`.

## Out of scope for this story

- Persisting the returned token (that's [[01-token-storage-helper]]).
- Auto-attaching the token to other requests (that's [[03-http-client-attaches-bearer]]).
- UI screens (separate stories).
