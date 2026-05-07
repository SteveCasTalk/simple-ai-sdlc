---
type: issue
feature: username-password-auth
lane: backend
status: ready
wave: 1
estimate: 45m
blocked-by:
  - "[[01-account-and-token-schema]]"
tags:
  - inception/issue
  - lane/backend
  - feature/username-password-auth
  - status/ready
  - wave/1
---

# [BE] `POST /auth/login` verifies credentials, issues a new token

**Lane:** Backend
**PRD section:** Goal 2, Story 2.
**API contract section:** §2 `POST /auth/login`.

## Why

Returning users need a way to get a fresh token. Same response shape as register, but only succeeds when the password matches the stored Argon2id hash.

## Acceptance criteria

- [ ] Valid `{username, password}` for an existing account returns `200` with `{token, account: {id, username}}`. The token is freshly created (not reused — multiple logins create multiple active tokens, supporting multi-device).
- [ ] Wrong password returns `401 invalid_credentials` (non-distinguishing per Decision D3).
- [ ] Unknown username returns the **same** `401 invalid_credentials` (intentional — see D3).
- [ ] Username lookup is case-insensitive (input is normalized to lowercase before query, matching how it was stored).
- [ ] Test: register an account → login with right password → 200; login with wrong password → 401; login with unknown username → 401 (same body as wrong password).
- [ ] Lane verify passes.

## Blocked by

- [[01-account-and-token-schema]]

## Hints (non-binding)

> [!tip]
> Reuse the password hash/verify helper introduced in [[02-register-endpoint]] (same library, same module). If 02 hasn't merged yet, Construction may need to coordinate or duplicate the helper temporarily — driver judgment call.

- **Likely files affected:** the same auth route group used by register; possibly extend the password helper.
- **Existing pattern to mirror:** the auth route added in [[02-register-endpoint]].
- **Watch out for:** **timing-safe** password comparison — Argon2id verify is timing-safe by construction, so as long as you use `verify(hash, candidate)` (not equality on rehashed strings), you're fine. Don't add naive `==` shortcuts.

## Out of scope for this story

- Logout (separate story).
- Refresh tokens / token rotation (per Decision D6, not in scope at all).
