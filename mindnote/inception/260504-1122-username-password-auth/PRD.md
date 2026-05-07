---
type: prd
feature: username-password-auth
status: draft
created: 2026-05-04
tags:
  - inception/prd
  - feature/username-password-auth
---

# PRD — Username + password authentication

> [!info] **One-line intent:** Let visitors self-register a username + password, log in, and use a long-lived opaque token to access their own notes — laying the foundation for per-account storage and future premium features.

## Why now

Until today every note in MindNote is anonymous and lives behind a default test account. To eventually support per-account note storage, multi-device sync, and premium features, the system needs a real notion of *who owns what*. Auth is the foundation slice.

## Goals (testable)

1. A new visitor can register a username + password and immediately get an auth token.
2. A returning user can log in with their username + password and get an auth token.
3. A logged-in user can log out, which revokes their current token server-side.
4. All existing endpoints (`/notes`, `/chat`, `/favorites`, etc.) require a valid `Authorization: Bearer <token>` header — unauthenticated calls return `401`.
5. Passwords are stored using Argon2id; the server never logs raw passwords.
6. The Android app sends the token automatically with every request after login, and clears the token on logout or 401.

## Non-goals (will be promoted to `out-of-scope.md`)

- Email-based identity (deferred — username-only for v1)
- Forgot-password / password reset (deferred — no recovery channel without email)
- Account deletion (deferred — not GDPR-relevant until EU users)
- MFA, OAuth/social login, magic links
- Premium feature gating, roles, entitlements (auth only — premium is a later feature)
- "Active sessions" UI (multi-device login works, but no user-facing list/manage)
- Email verification, captcha, anti-bot

## User stories

### Story 1 — Self-register

**As** a new visitor opening the app for the first time,
**I want to** create an account with a username + password,
**So that** I can start using MindNote with notes that belong to me.

**Acceptance:**
- Register screen shows username, password, and password-confirm fields.
- Password rules are visible (length ≥ 8, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special) with live validation feedback.
- On valid submit → `POST /auth/register` → on 201, token persisted, navigate to Home.
- On 409 (username taken) → inline error, retry without losing input.
- On 400 (validation failed) → inline field-level errors.

### Story 2 — Log in

**As** a returning user,
**I want to** sign in with my username + password,
**So that** my notes appear and I can keep working.

**Acceptance:**
- Login screen shows username + password.
- On valid submit → `POST /auth/login` → on 200, token persisted, navigate to Home.
- On 401 (bad credentials) → inline error "username or password is wrong" (intentional non-distinguishing — see open-questions Q2).
- "Don't have an account? Register" link navigates to the register screen.

### Story 3 — Log out

**As** a logged-in user,
**I want to** log out from a settings/menu action,
**So that** my token is invalidated and I'm returned to the login screen.

**Acceptance:**
- Logout action exists in the app (settings menu, profile area — Construction picks the placement).
- On tap → `POST /auth/logout` (server revokes current token) → local token cleared → navigate to Login.
- Subsequent API calls (after logout but with old token cached anywhere) return `401` and are ignored gracefully.

### Story 4 — Authenticated app entry

**As** a user re-opening the app,
**I want** the app to show my notes immediately if I'm still logged in, or the login screen if I'm not,
**So that** I'm never stuck or asked to re-auth needlessly.

**Acceptance:**
- App startup checks for a stored token.
- Token present → goes straight to Home.
- Token absent → goes to Login.
- Token present but server returns `401` (revoked or invalid) → token cleared, navigated to Login.

### Story 5 — Note isolation by account

**As** a logged-in user,
**I want** to see only my own notes — not anyone else's,
**So that** privacy is respected.

**Acceptance:**
- All `/notes` reads/writes scope by the authenticated account's `id`.
- A second account, registered separately, sees zero notes initially and cannot read account A's notes by guessing IDs.
- Test: create two accounts, post a note from A, list notes as B → B's list is empty; B requests A's note by id → 404 (or 403 — Construction picks).

## Success metrics

For v1, success is binary and end-to-end-test-driven:

- **End-to-end test:** register → receive token → call `POST /notes` → call `GET /notes` → call `POST /auth/logout` → call `GET /notes` returns 401.
- **Regression check:** all existing endpoints still work with valid auth; all return 401 without auth.
- **Security smoke:** server logs do not contain raw passwords (grep for "password" in logs after running the e2e test → no plaintext-looking values).

No product KPI for v1. The feature is "successful" when the e2e test passes and a second account can register without cross-contamination.

## Out of scope for this feature

See [out-of-scope.md](out-of-scope.md).
