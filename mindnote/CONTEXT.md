---
type: context
project: mindnote
tags:
  - context
  - project/mindnote
---

# CONTEXT

> [!note] **Project-wide.** This file lives at the **project root**, not inside any feature's Inception folder. It is the project's shared language and grows across features. Each feature's Inception run *appends* to it.

Shared language for this project. Every term here must earn its place by replacing a longer phrase the team would otherwise repeat. **If a term is only used once, delete it.**

## Domain terms

| Term | Meaning | Replaces |
|---|---|---|
| **Scan flow** | The user journey from the Home Scan FAB through image source selection, OCR, review, and note save. | "the photo-OCR-to-note pipeline on Android" |
| **Extracted text** | OCR output returned by the server, before any user edits. Distinct from the note's final `body` (which the user may edit). | "the OCR result before review" |
| **OCR provider** | The third-party or self-hosted text-recognition engine the server delegates to. Hidden behind a server-side abstraction so we can swap implementations without touching the API contract. | "the OCR backend / vendor / engine" |
| **Source photo** | The original image a user picked or captured for OCR. Stored locally on the device alongside the resulting note; not uploaded for storage (only transient upload to OCR endpoint). | "the original image attachment" |
| **Account** | A registered user record (`username` + password hash). The owner of notes — every note now belongs to exactly one account. | "user", "the logged-in person" |
| **Auth token** | An opaque, server-issued string the client sends as `Authorization: Bearer <token>`. Long-lived; revoked only by explicit logout. Distinct from any third-party OAuth token. | "session", "API key", "JWT" |
| **Self-register** | The flow where a new visitor creates their own account via `POST /auth/register`, no admin in the loop. | "sign up" |
| **Password rule set** | The five register-time constraints: length ≥ 8, ≥1 upper, ≥1 lower, ≥1 digit, ≥1 special. | "password complexity rules" |
| **Block** | A unit of content inside a `Note.body`. Either `prose` (paragraph) or `item` (checklist item). Added by checklists-with-decay; replaces the previous "body is one big string" model. | "the prose-or-item segment of a note" |
| **Touch** | Any user interaction that resets `ChecklistItem.last_touched_at` to now: toggle done / edit text / reorder. Opening or scrolling does not count. | "user re-engaged with this item" |
| **Decay** | The visual fade applied to `ChecklistItem`s based on age since `last_touched_at`. Linear opacity 100% → 20% over 30 days, then constant until 60d auto-archive. | "the fading-over-time UX" |
| **Auto-archive** | Daily server-side process (cron 03:00 UTC) that sets `archived_at = now` on items 60+ days untouched. Both done and undone items archive. | "the 60-day cleanup pass" |
| **Rescue** | The user action that un-archives a `ChecklistItem`: clears `archived_at`, resets `last_touched_at = now`, returns it to the live list at full opacity. | "undo the auto-archive" |
| **Live list** | A note's `ChecklistItem`s where `archived_at IS NULL`. Rendered above the optional `▶ Show N archived` expander. | "the unarchived items in a note" |

## Domain entities (data model)

### Note

- **What it is:** A user's saved piece of text content.
- **Identifier:** `id` (string, format `n-<epoch-ms>` when created locally).
- **Key fields:** `title`, `preview`, `body`, `tags: List<String>`, `date: LocalDate`, **`imagePath: String?`** (Android-only — points at a file under `filesDir/ocr/` for notes created via the Scan flow; null for manually-captured notes).
- **NEW field for the auth feature:** `user_id: String` (server-side; FK → `Account.id`). All `/notes` reads/writes scope by the authenticated account.
- **Lifecycle:** Created via the Capture screen (manual entry, `imagePath = null`) or the Scan flow (OCR, `imagePath` set). Soft state lives in Room on the device and mirrors via the `/notes` endpoint on the server. The server's `NoteDto` does not carry `imagePath`; on refresh from server, the Android mapping preserves any existing local `imagePath`.
- **Relationships:** `Note → Account` (many-to-one); `Note ↔ Topic` (many-to-many through `NoteTopics`); `Favorite` references `Note.id`.
- **Migration note (auth feature):** Existing notes are wiped along with the test users — no data preservation.

### Account (new — added by the username-password-auth feature)

- **What it is:** A registered user record. Owns zero or more notes.
- **Identifier:** `id: String`
- **Key fields:**
  - `username: String` — 3–30 chars, `[a-z0-9_]`, lowercase, globally unique.
  - `password_hash: String` — Argon2id encoded string (includes salt + algorithm parameters).
  - `created_at: Instant`
- **Lifecycle:** Created via `POST /auth/register`. Account deletion not in scope.

### AuthToken (new — added by the username-password-auth feature)

- **What it is:** A server-side record mapping an opaque token string to an Account.
- **Identifier:** `token: String` (URL-safe, ≥128 bits of entropy).
- **Key fields:** `token`, `account_id` (FK → Account), `created_at`.
- **Lifecycle:** Created on `POST /auth/login` and `POST /auth/register`. Deleted on `POST /auth/logout`. No expiry — long-lived until explicit logout.
- **Relationships:** Many tokens per account (one per device login). Logout revokes only the *current* token (the one in the request header), not all tokens for the account.

### Block (new — added by the checklists-with-decay feature)

- **What it is:** The unit of content inside `Note.body`. As of this feature, `Note.body` is no longer a plain string — it is a list of `Block`s. Two block types exist: `prose` (a paragraph of text) and `item` (a checklist item).
- **Identifier:** Position in the list. Prose blocks have no stable ID; item blocks carry their own `id` (see `ChecklistItem`).
- **Key fields:** `type: "prose" | "item"`. Prose has `text`. Item carries the fields listed under `ChecklistItem`.
- **Lifecycle:** Prose blocks are created/edited/deleted via `PATCH /notes/{id}` (which replaces the full `body`). Item blocks have their own item-scoped endpoints to make `Touch` semantics atomic.
- **Migration note:** Pre-existing notes' string `body` is migrated to a single `[{type: "prose", text: <old body>}]` block on the first read. No data loss (unlike the auth feature's wipe).

### ChecklistItem (new — added by the checklists-with-decay feature)

- **What it is:** A first-class checklist item embedded in a note as an `item`-type Block. Carries text, done state, and decay metadata. The whole reason this feature exists.
- **Identifier:** `id: String` — server-issued, opaque, globally unique across all items.
- **Key fields:**
  - `text: String` — the item's content.
  - `done: Boolean` — checkbox state.
  - `last_touched_at: Instant` — last `Touch`. Drives `Decay` opacity. Server-authoritative; clients never send it.
  - `archived_at: Instant?` — set by `Auto-archive` at 60d untouched, or by the cron. Cleared by `Rescue`.
- **Lifecycle:** Created via `POST /notes/{id}/items`; mutated via `PATCH /notes/{id}/items/{item_id}` (any successful mutation resets `last_touched_at = now`); auto-archived by the daily cron; restored via `POST /notes/{id}/items/{item_id}/rescue`; hard-deleted via `DELETE`.
- **Relationships:** `ChecklistItem → Note` (many-to-one). Account ownership flows through the parent note.

## Glossary of process terms

_(none yet — all "Touch / Decay / Auto-archive / Rescue / Live list" terms live in the Domain terms table above since they're domain concepts the team will use in conversation, not internal process names.)_
