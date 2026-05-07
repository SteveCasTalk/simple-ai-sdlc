---
type: issue
feature: username-password-auth
lane: backend
status: ready
wave: 1
estimate: 30m
blocked-by:
  - "[[01-account-and-token-schema]]"
tags:
  - inception/issue
  - lane/backend
  - feature/username-password-auth
  - status/ready
  - wave/1
---

# [BE] `POST /auth/logout` revokes the current token

**Lane:** Backend
**PRD section:** Goal 3, Story 3.
**API contract section:** §3 `POST /auth/logout`.

## Why

Closes the loop: server-side token revocation so a logged-out token can never be used again, even if it leaks. Single-token revocation per Decision D6 (not "logout everywhere").

## Acceptance criteria

- [ ] Valid `Authorization: Bearer <token>` → `204 No Content` and the token is removed from the auth tokens table.
- [ ] After a successful logout, calling any Bearer-gated endpoint with the same token returns `401`.
- [ ] Missing or unknown `Authorization` header → `401 invalid_token` (or `unauthenticated`) — the endpoint does not error 500.
- [ ] Test: register → use token to call a Bearer-gated endpoint successfully → logout → use same token → 401.
- [ ] Lane verify passes.

## Blocked by

- [[01-account-and-token-schema]]

## Hints (non-binding)

> [!tip]
> This story does NOT install the Bearer middleware on existing routes — that's [[05-bearer-auth-on-existing-routes]]. But it DOES need to read the token from the request to know what to revoke. Construction picks: (a) require Bearer middleware to be merged first and depend on the `Account` it injects, or (b) parse the header inline (simpler, scope-limited to this endpoint).

- **Likely files affected:** the same auth route group as register/login.
- **Existing pattern to mirror:** see the auth routes from [[02-register-endpoint]] / [[03-login-endpoint]] for the route extension pattern.

## Out of scope for this story

- Logout-everywhere ("revoke all tokens for this account") — see [out-of-scope](out-of-scope.md).
- Audit logging of logout events.
