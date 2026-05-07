---
type: open-questions
feature: username-password-auth
status: open
created: 2026-05-04
tags:
  - inception/open-questions
  - feature/username-password-auth
---

# Open questions

> [!warning] **For the mob.** These don't block Construction starting on the unblocked stories — Construction can ship the core slice without resolving them. They DO need a team call before this feature can be considered "done" in production.

## Open

### Q1 — Login rate limiting

With open self-registration + long-lived tokens, brute-force on the login endpoint is a real risk. Options:

- **(a) None in v1** — accept the risk; revisit when first attack happens. *(Driver guess: leans here for v1 ASAP.)*
- **(b) Per-IP throttle in the server** — e.g., 10 failed logins per IP per 15 min. Adds a dependency or in-memory store + a route filter.
- **(c) Edge-layer throttle** — Railway / Cloudflare in front of the server. Zero server code; depends on Railway capability.
- **(d) Per-account lockout** — N failed attempts → lock account for M minutes. Adds a `locked_until` column; opens a denial-of-service vector (attacker locks out a known username on purpose).

**Mob to decide.** If (b), flag a Construction story to add it before launch.

### Q2 — Username enumeration on `/auth/register`

Per [decisions](decisions.md) D2, the driver chose **(a) accept the leak** — `409 username_taken` is distinct from `400 validation_failed`. The mob may overturn:

- (a) Distinguish (current decision) — better UX, accepts username enumeration.
- (b) Generic "registration failed" — prevents enumeration, hurts UX.
- (c) Distinguish, but with rate limiting on `/auth/register` to make enumeration slow — middle ground; ties into Q1.

### Q3 — Token revocation visibility

Decision D6: logout revokes *only the current* token. Mob to ratify or push back:

- Should we *also* surface a "log out everywhere" action in v1 (cheap to add — server-side `DELETE FROM auth_tokens WHERE account_id = ?`)?
- Should we surface "active sessions" UI later? (Out of scope for this feature, but worth a placeholder decision.)

## Resolved

_(none yet — will fill as mob answers above)_
