---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 1
estimate: 45m
blocked-by:
  - "[[01-token-storage-helper]]"
tags:
  - inception/issue
  - lane/android
  - feature/username-password-auth
  - status/ready
  - wave/1
---

# [Android] App startup routes to Home if a token exists, Login if it doesn't

**Lane:** Android
**PRD section:** Story 4 (authenticated app entry).
**API contract section:** n/a.

## Why

Without this, users see Home even when logged out (and get 401s on every call), or are forced through login on every launch. The branching at startup is the user-observable result of "auth survives app restart".

## Acceptance criteria

- [ ] On app launch, the nav graph reads the stored token and routes to Home if present, Login (placeholder OK if A-5 isn't merged) if absent.
- [ ] Once Login (or Register) completes successfully, the user lands on Home — never back at Login.
- [ ] Once Logout completes, the user lands on Login — never back at Home.
- [ ] Test: instrumentation or compose-test that asserts the start destination based on the token state.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]] (needs token-read).

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md) — single-activity Compose with `androidx.navigation.compose`. Routes registered in `core/navigation/`. The decision of "where do we start?" is typically made in `MindNoteNavHost` or whatever the nav root is.

- **Likely files affected:** `core/navigation/` (add Login/Register routes if not present, or placeholder ones); the nav-host / `MainActivity` / app entry composable.
- **Existing pattern to mirror:** how other start-destination-conditional flows work (e.g. onboarding-vs-home if such logic exists).
- **Watch out for:** if the token read is async (suspending / Flow), the start-destination decision must wait for the first emission — show a small splash or `Box {}` placeholder to avoid a flash of the wrong screen.

## Out of scope for this story

- The actual Login / Register screen content (separate stories — placeholder composables are fine here).
- Logout action (separate story).
- 401-driven re-routing back to Login (separate story).
