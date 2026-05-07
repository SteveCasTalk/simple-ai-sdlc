---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 2
estimate: 45m
blocked-by:
  - "[[01-token-storage-helper]]"
  - "[[03-http-client-attaches-bearer]]"
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/2
---

# [Android] A 401 from any API call clears the token and bounces the user to Login

**Lane:** Android
**PRD section:** Story 4 acceptance: "token present but server returns 401 (revoked or invalid) → token cleared, navigated to Login."
**API contract section:** §4 (Bearer requirement on existing endpoints).

## Why

A token can become invalid without the user logging out (server-side revocation, manual DB cleanup, expired-by-policy in the future). When that happens, the next API call returns 401 — the app should treat it as "log out and go back" rather than showing an error toast on a now-permanently-broken Home screen.

## Acceptance criteria

- [ ] Any API call that returns `401` (any code: `unauthenticated`, `invalid_token`) triggers: clear local token → navigate the app to Login.
- [ ] The user-visible result is: the user lands on Login, ideally with a one-shot toast / banner "You've been signed out. Please log in again." (Construction picks the copy.)
- [ ] The 401 from `/auth/login` itself does NOT trigger this flow (login expects 401 as the "wrong credentials" signal — see [[05-login-screen-and-flow]]). Construction either scopes the interceptor to skip `/auth/*` or distinguishes by call site.
- [ ] Test: with MockEngine, fire a request that returns 401 → assert the token store is cleared and a navigate-to-login signal is emitted.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]]
- [[03-http-client-attaches-bearer]]

## Hints (non-binding)

> [!tip]
> Ktor's `HttpClient` supports response observers / plugins (e.g. `HttpResponseValidator`) that can react to status codes globally. That's typically the right home for this — installed in the same `HttpClient` builder block as the bearer-attach logic.

- **Likely files affected:** `core/di/AppModule.kt` (extend the `HttpClient` builder); a tiny "auth-revoked" event channel (Flow / SharedFlow / event bus — Construction picks the existing pattern in the app).
- **Watch out for:** the navigate-to-login can't happen from inside the HttpClient (no Compose / nav scope) — it must emit an event the nav layer subscribes to.

## Out of scope for this story

- Refresh token flow (per Decision D6, no refresh tokens).
- "Reconnect" UI on transient network failures (this story is specifically about 401, not 5xx or no-network).
