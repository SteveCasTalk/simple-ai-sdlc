---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 2
estimate: 30m
blocked-by:
  - "[[01-token-storage-helper]]"
  - "[[02-auth-api-client]]"
  - "[[03-http-client-attaches-bearer]]"
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/2
---

# [Android] User can log out — token is revoked server-side, cleared locally, navigated back to Login

**Lane:** Android
**PRD section:** Story 3 (log out).
**API contract section:** §3 `POST /auth/logout`.

## Why

Without an explicit logout, the only way out is uninstalling the app. Logout is also the way users move accounts on a shared device.

## Acceptance criteria

- [ ] A Logout action exists somewhere reachable from Home (Construction picks placement — settings, profile menu, etc.).
- [ ] Tapping Logout: calls the logout API → clears the local token → navigates to Login.
- [ ] If the logout API returns an error (network, 401), the local token is STILL cleared and the user is STILL navigated to Login. (Failing-open is correct here: the user wanted out.)
- [ ] After logout, opening any screen that needs auth → Login (verified by app-entry routing from [[04-app-entry-routing]]).
- [ ] Test: ViewModel-level — happy logout calls API → clears token → emits navigate-to-login. Network error → still clears token → still emits navigate-to-login.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]]
- [[02-auth-api-client]]
- [[03-http-client-attaches-bearer]] (logout call needs the bearer to be auto-attached)

## Hints (non-binding)

- **Likely files affected:** wherever the logout entry point lives (likely a settings / profile screen — Construction picks); plus a small piece of logic that wraps `AuthApi.logout() → clear token → navigate`.
- **Existing pattern to mirror:** how other navigate-after-action flows work in the app.
- **Watch out for:** clearing the token must happen BEFORE the nav transition completes — otherwise a brief race where the next screen makes a request with the old token is possible (cosmetic 401, but ugly).

## Out of scope for this story

- "Logout everywhere" (revoke all tokens for the account) — deferred.
- A confirmation dialog ("Are you sure?") — Construction can add if it fits the design system, otherwise skip.
