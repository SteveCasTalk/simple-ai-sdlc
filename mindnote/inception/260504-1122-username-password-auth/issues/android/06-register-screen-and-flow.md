---
type: issue
feature: username-password-auth
lane: android
status: ready
wave: 2
estimate: 90m
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

# [Android] User can register with username + password (with live validation) and land on Home

**Lane:** Android
**PRD section:** Story 1 (self-register).
**API contract section:** §1 `POST /auth/register`.

## Why

The first thing a brand-new user does. Live password-rule feedback is the highest-friction part of self-register; getting it right is the difference between "I'm in!" and "I gave up after the third try."

## Acceptance criteria

- [ ] Register screen exists with: username, password, confirm-password fields, and a Register button.
- [ ] **Live password validation:** as the user types in the password field, each of the five rules (length ≥ 8, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special) is shown as a checklist with check / cross icons updating in real time.
- [ ] Confirm-password field shows an inline error when it doesn't match (after first interaction; not on initial render).
- [ ] Register button is disabled until: username matches `^[a-z0-9_]{3,30}$` AND all five password rules are satisfied AND confirm-password matches.
- [ ] On submit → call register API → on `201` → token saved → navigate to Home.
- [ ] On `409 username_taken` → inline error on username field "Username already taken" → input retained.
- [ ] On `400 validation_failed` → inline field-level error (server-side validation is the source of truth — client-side validation is UX, not security).
- [ ] Tests: ViewModel-level for password-rule state computation, happy register, 409, 400, network error.
- [ ] Lane verify passes.

## Blocked by

- [[01-token-storage-helper]]
- [[02-auth-api-client]]
- [[04-app-entry-routing]]

## Hints (non-binding)

> [!tip]
> See [CLAUDE.md](CLAUDE.md). The MVI pattern fits well here: a `RegisterContract` exposes `state.passwordChecks: PasswordChecks` (a small data class with five booleans) and the View renders the checklist directly from that.

- **Likely files affected:** new `features/auth/register/` package (mirroring login).
- **Existing pattern to mirror:** [[05-login-screen-and-flow]] for screen / VM shape; existing onboarding for any multi-field form patterns.
- **Watch out for:** **username case** — input should be auto-lowercased OR display-cased but submitted lowercased. Pick one; document in the commit. Per the api-contract, server normalizes to lowercase regardless.

## Out of scope for this story

- Email field (deferred — username-only per Decision D6's omission).
- Password-strength meter beyond the 5-rule checklist (sufficient for v1).
- Captcha / anti-bot (deferred — see [out-of-scope](out-of-scope.md)).
