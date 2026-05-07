---
type: out-of-scope
feature: username-password-auth
status: draft
created: 2026-05-04
tags:
  - inception/out-of-scope
  - feature/username-password-auth
---

# Out of scope

> [!info] **Belt and suspenders.** Things explicitly excluded from this feature so the mob doesn't relitigate them. Each entry should be one line stating *what* and a hint at *when*/*how* it might come back.

## Identity & recovery

- **Email as identity** — username-only for v1. Email as a linkable secondary identity is a future feature. Touchpoint: an `account.email` column would be added then, nullable.
- **Forgot password / password reset** — deferred. Needs an email channel, which v1 doesn't have.
- **Account deletion** — deferred. GDPR-relevant only when EU users exist.
- **Username change** — deferred. Breaks display references; needs careful migration.

## Auth methods

- **MFA / 2FA** — deferred. No security-sensitive features yet (no payments, no health data).
- **OAuth / social login** (Google, Apple, GitHub) — deferred. Big surface area; revisit when onboarding friction is measured.
- **Magic links / passwordless** — deferred. Requires email.
- **Biometric unlock** (fingerprint / face) — deferred. App-level UX; doesn't replace server auth.

## Session & security

- **Token expiry / refresh tokens** — by Decision D6, tokens are long-lived. Time-based expiry + refresh model is a future change if a security incident or audit demands it.
- **"Log out everywhere"** (revoke all tokens for an account) — deferred. Useful when account compromise is suspected; not v1.
- **Active sessions UI** (list/manage devices currently logged in) — deferred. Companion to "log out everywhere".
- **Login rate limiting** — see [open-questions](open-questions.md) Q1; deferred unless mob picks an answer that demands per-IP throttle infra in v1.
- **Account lockout** after N failed logins — deferred. Coupled with rate-limiting decision.
- **Captcha / anti-bot on register** — deferred. Real-world friction-vs-spam trade-off; reassess when bots show up.
- **Password breach check** (HaveIBeenPwned API) — deferred. Nice-to-have; adds external dependency.

## Authorization & roles

- **Roles / permissions / admin user** — deferred. Single role ("user") in v1.
- **Premium feature gating / entitlements** — separate later feature. This slice produces auth only.
- **Per-account quotas** (notes count, storage MB) — deferred.

## Operational

- **Audit log** of auth events (registers, logins, logouts) — deferred. Useful for incident response; not v1.
- **Sign-in notification email** ("new sign-in from device X") — deferred. Needs email channel.
- **GDPR data export** — deferred until EU users.
