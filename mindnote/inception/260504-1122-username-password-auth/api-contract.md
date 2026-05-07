---
type: api-contract
feature: username-password-auth
status: draft
created: 2026-05-04
tags:
  - inception/api-contract
  - feature/username-password-auth
---

# API contract — username-password-auth

> [!success] **No `TBD`s remaining** — Backend and Android can proceed against this contract in parallel.

## Conventions

- All requests / responses use JSON (`Content-Type: application/json`).
- Authenticated endpoints require `Authorization: Bearer <token>` (where `<token>` is the opaque string from `/auth/register` or `/auth/login`).
- Error envelope (server-wide):
  ```json
  { "error": { "code": "<machine_code>", "message": "<human-readable>" } }
  ```
  Construction may extend per-error with extra fields; `code` and `message` are required.

## Endpoints

### 1. `POST /auth/register`

Self-register a new account.

**Auth:** none.

**Request:**
```json
{
  "username": "alice_42",
  "password": "Hunter2!aB"
}
```

**Validation (server-enforced; client should also validate for UX):**
- `username`: 3–30 chars, regex `^[a-z0-9_]+$` (lowercase letters, digits, underscore). Stored lowercase.
- `password`: length ≥ 8, ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special character (`!@#$%^&*()_+-=[]{};:'",.<>/?\|~\``).

**Responses:**
- `201 Created`:
  ```json
  { "token": "<opaque-string>", "account": { "id": "<acct-id>", "username": "alice_42" } }
  ```
  The token is *the same shape* as a login token — the new user is logged in.
- `400 Bad Request`:
  ```json
  { "error": { "code": "validation_failed", "message": "...", "fields": { "password": "must contain a digit" } } }
  ```
- `409 Conflict`:
  ```json
  { "error": { "code": "username_taken", "message": "username is already in use" } }
  ```
  *(Note: this leaks username existence — see decisions D2.)*

---

### 2. `POST /auth/login`

Exchange username + password for a token.

**Auth:** none.

**Request:**
```json
{ "username": "alice_42", "password": "Hunter2!aB" }
```

**Responses:**
- `200 OK`:
  ```json
  { "token": "<opaque-string>", "account": { "id": "<acct-id>", "username": "alice_42" } }
  ```
- `401 Unauthorized`:
  ```json
  { "error": { "code": "invalid_credentials", "message": "username or password is wrong" } }
  ```
  *(Intentionally non-distinguishing — same response whether username doesn't exist or password is wrong. See decisions D3.)*

---

### 3. `POST /auth/logout`

Revoke the current token server-side.

**Auth:** Bearer (the token being revoked).

**Request:** empty body.

**Responses:**
- `204 No Content` — token deleted from server.
- `401 Unauthorized` — token missing/invalid (no-op effectively; client clears local token regardless).

---

### 4. All existing endpoints — Bearer requirement added

The following existing endpoints become **Bearer-gated** in this feature (no shape changes — only the auth header becomes mandatory):

- `GET /notes`, `POST /notes`, `GET /notes/{id}`, `DELETE /notes/{id}`, `GET /notes/{id}/related`
- `GET /favorites`, `POST /favorites/...`, `DELETE /favorites/...`
- `POST /chat`, `GET /chat/history`, etc.
- (Any other route currently in `Routes.kt` / `ChatRoutes.kt` — Construction confirms by reading those files.)

**Exempt routes** (never gated):
- `POST /auth/register`
- `POST /auth/login`
- `GET /health` (or whatever health endpoint exists)

**Behavior:**
- Missing `Authorization` header → `401`:
  ```json
  { "error": { "code": "unauthenticated", "message": "auth required" } }
  ```
- Header present but token unknown/revoked → `401` (same envelope, code `invalid_token`).
- Header present, token valid → handler runs as before; the resolved Account is available to the handler (e.g. via Ktor call attribute) so handlers can scope queries by `account_id`.

## Notes for Construction

- The token is opaque to the client. Don't try to parse it. Store as-is, send as-is.
- Token persistence on Android: per Decision D4 — DataStore Preferences (existing `UserPrefs` pattern). Plaintext on device is acceptable trade-off for v1.
- Server-side argon2id parameters: pick safe modern defaults (e.g. `t=3, m=65536, p=4`) — Construction may revise if the chosen library has different recommendations. Document the choice in the BE story implementation commit.
