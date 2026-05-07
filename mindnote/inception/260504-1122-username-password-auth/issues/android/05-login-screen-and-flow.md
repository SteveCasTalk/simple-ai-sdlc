---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 2
estimate: 75m
blocked-by:
  - "[[01-token-storage-helper]]"
  - "[[02-auth-api-client]]"
  - "[[04-app-entry-routing]]"
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/2
---

# [Android] User can log in with username + password and land on Home

**Lane:** Android
**PRD section:** Story 2 (log in).
**API contract section:** §2 `POST /auth/login`.

## Why

The end-to-end happy path for a returning user. Ties together the API client, token storage, and app routing into one user-observable flow.

## Acceptance criteria

- [ ] Login screen exists with two text fields (username, password — password masked) and a Login button.
- [ ] Tapping Login with valid credentials → call to login API → on success → token saved → navigate to Home.
- [ ] Tapping Login with invalid credentials → inline error "Username or password is wrong" (matches the non-distinguishing wording from Decision D3) → input retained, password field cleared.
- [ ] Tapping Login with a network error → user-visible inline error "Couldn't reach the server" (or similar), retry possible.
- [ ] Login button is disabled while a request is in flight; some visible busy state (spinner / disabled) is shown.
- [ ] A "Don't have an account? Register" affordance navigates to the Register screen.
- [ ] Tests: ViewModel-level tests for: (a) happy path → emits navigate-to-home effect + persists token, (b) 401 → emits invalid-credentials state, (c) network error → emits network-error state, (d) double-tap during in-flight request doesn't fire two API calls.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]]
- [[02-auth-api-client]]
- [[04-app-entry-routing]] (so login can navigate to Home properly)

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md) — Compose Material3, MVI pattern in `core/mvi`, Koin viewModel registration, screens under `features/`. New feature folder: `features/auth/login/` with `LoginScreen.kt`, `LoginViewModel.kt`, `LoginContract.kt`.

- **Likely files affected:** new `features/auth/login/` package; `core/di/AppModule.kt` for the ViewModel registration; `core/navigation/` for the route.
- **Existing pattern to mirror:** any existing screen with a form (e.g. `CaptureScreen` or `OnboardingViewModel`).
- **Watch out for:** the password field should not be in IME suggestions / autofill in unsafe modes — use the standard Compose password TextField semantics.

## Out of scope for this story

- Register flow (separate story).
- Forgot-password (deferred — see [out-of-scope](out-of-scope.md)).
- Biometric / autofill integration (deferred).
