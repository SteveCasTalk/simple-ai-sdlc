---
type: api-contract
feature: checklists-with-decay
created: 2026-05-07
backend-work: true
tags:
  - inception/api-contract
  - feature/checklists-with-decay
---

# API contract — Checklists with decay

> [!warning] **Keystone artifact.**
> When this file has zero `TBD` markers, BE / Android can proceed in parallel.

## Conventions

- **Auth:** Bearer token (existing — `Authorization: Bearer <token>`). All endpoints below require auth.
- **Base URL:** as configured (Railway-deployed Ktor server). Existing.
- **Error envelope:** `{ "error": { "code": string, "message": string, "details": object? } }` — existing.
- **Date format:** ISO 8601 UTC, e.g. `2026-05-07T14:45:00Z`.
- **Touch semantics:** *every successful mutating call on an item resets that item's `last_touched_at = now`.* Server is the timekeeping authority; client never sends `last_touched_at`.

## Block shape (used in note `body`)

The single shape used everywhere `body` appears in requests/responses below.

```json
// Prose block
{ "type": "prose", "text": "string" }

// Item block (read shape — what server returns)
{
  "type": "item",
  "id": "string",
  "text": "string",
  "done": false,
  "last_touched_at": "2026-05-07T14:45:00Z",
  "archived_at": null
}

// Item block (write shape — what client sends in note CRUD)
// Used only when creating a note with items, or when the client wants
// to replace the whole body. Per-item updates use the dedicated item endpoints.
{ "type": "item", "id": "string?", "text": "string", "done": false }
```

| Field | Type | Notes |
|---|---|---|
| `type` | `"prose" \| "item"` | Discriminator. |
| `text` | string | Required for both block types. |
| `id` | string | Item-only. Server-issued. Optional in write payloads only when creating a brand-new item; required when updating. |
| `done` | boolean | Item-only. Default `false`. |
| `last_touched_at` | ISO 8601 | Item-only. Read-only — server populates. |
| `archived_at` | ISO 8601 \| null | Item-only. Read-only — server populates. |

---

## Endpoints

### `GET /notes/{id}` — fetch a note (modified shape)

**Auth:** required.

**Query params:**

| Param | Type | Required | Notes |
|---|---|---|---|
| `include_archived` | boolean | no | Default `false`. When `true`, archived items are included inline in `body` at their stored positions, with `archived_at` populated. |

**Success response (200):**

```json
{
  "id": "n-1746619500000",
  "title": "Tuesday plans",
  "preview": "string (first 100 chars of first prose block)",
  "date": "2026-05-07",
  "body": [
    { "type": "prose", "text": "Brain dump from this morning:" },
    { "type": "item", "id": "i-abc123", "text": "Call dentist", "done": false, "last_touched_at": "2026-05-05T09:12:00Z", "archived_at": null },
    { "type": "item", "id": "i-def456", "text": "Refactor NotesScreen", "done": true,  "last_touched_at": "2026-05-06T22:00:00Z", "archived_at": null },
    { "type": "prose", "text": "Then think about Q3 roadmap." }
  ],
  "tags": ["personal"],
  "archived_count": 3
}
```

| Field | Type | Notes |
|---|---|---|
| `body` | `Block[]` | Live (non-archived) blocks only when `include_archived=false`. |
| `archived_count` | int | Count of archived items in this note. Drives the `▶ Show N archived` expander. |

**Error responses:**

| Code | When |
|---|---|
| 401 | missing/expired token |
| 404 | note not found or not owned by caller |

**Notes:**
- Idempotency: read-only, idempotent.
- Pagination: none (fetches a whole note).

---

### `GET /notes` — list notes (modified shape)

**Auth:** required.

Same modifications as `GET /notes/{id}`: each note in the response now carries `body: Block[]` and `archived_count: int`. The list-level paging behavior is unchanged from the existing endpoint.

**Notes:**
- `body` for a list response *may* be truncated by Construction to a fixed prefix (e.g. first N blocks) for performance, if measurements show full bodies are too heavy. If so, response includes a per-note `body_truncated: bool`. **TBD — see [[open-questions#Q2]].**

---

### `POST /notes` — create a note (modified shape)

**Auth:** required.

**Request:**

```json
{
  "title": "string",
  "date": "2026-05-07",
  "tags": ["string"],
  "body": [
    { "type": "prose", "text": "string" },
    { "type": "item", "text": "string", "done": false }
  ]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `body` | `Block[]` | yes | Item blocks omit `id` (server assigns). `last_touched_at`/`archived_at` ignored if sent. |

**Success response (201):** the created note in the same shape as `GET /notes/{id}`. All item blocks have server-issued `id` and `last_touched_at = now`, `archived_at = null`.

---

### `PATCH /notes/{id}` — update note metadata + replace body

**Auth:** required.

**Request (any subset of fields):**

```json
{
  "title": "string",
  "date": "2026-05-07",
  "tags": ["string"],
  "body": [ /* full block list */ ]
}
```

**Behavior:**
- If `body` is included, server **replaces** the full block list. For prose blocks, this is straightforward. For item blocks: items present in the new list keep their `last_touched_at` and `archived_at` *unless* their `text` or `done` changed in this request — in which case `last_touched_at = now`. Items present before but absent now are deleted (hard).
- Items omitted from `body` because the client only intended to update prose are **deleted**. To preserve items, the client must include them in `body` with their existing `id`s. Construction may add `?preserve_items=true` if testing reveals this footgun is too sharp — **TBD — see [[open-questions#Q3]].**
- Per-item updates (toggle done, edit single item text, reorder) **should use the dedicated item endpoints below**, not this endpoint, to keep `Touch` semantics atomic and avoid the omit-deletes-item footgun.

**Success response (200):** updated note.

---

### `POST /notes/{id}/items` — append a new item

**Auth:** required.

**Request:**

```json
{
  "text": "string",
  "position": 3
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `text` | string | yes | Initial text. |
| `position` | int | no | Insert position within `body`. Default: append to end. 0-based. |

**Success response (201):** the created item block.

```json
{ "type": "item", "id": "i-abc123", "text": "Call dentist", "done": false, "last_touched_at": "2026-05-07T14:45:00Z", "archived_at": null }
```

---

### `PATCH /notes/{id}/items/{item_id}` — update item (text / done / position)

**Auth:** required.

**Request (any subset):**

```json
{
  "text": "string",
  "done": true,
  "position": 5
}
```

**Behavior:**
- **Any successful call resets `last_touched_at = now`** — even if `done` is set to its current value (idempotent in data, but counts as a `Touch`).
- If the item is currently archived (`archived_at != null`), this endpoint returns `409 Conflict` — clients must call `/rescue` first. *(See [[open-questions#Q4]] — alternative is "PATCH on archived item silently rescues.")*

**Success response (200):** updated item block.

**Error responses:**

| Code | When |
|---|---|
| 400 | invalid field (e.g. `position` out of bounds) |
| 404 | note or item not found |
| 409 | item is archived; call `/rescue` first |

---

### `DELETE /notes/{id}/items/{item_id}` — delete an item

**Auth:** required.

Hard delete. Returns `204 No Content`. Idempotent — deleting a non-existent item returns `204` (not `404`). *(Idempotency choice flagged as [[open-questions#Q5]].)*

---

### `POST /notes/{id}/items/{item_id}/rescue` — un-archive + reset timer

**Auth:** required.

**Request:** empty body.

**Behavior:** sets `archived_at = null`, sets `last_touched_at = now`. Item returns to the live list at full opacity. Idempotent — calling rescue on a non-archived item is a no-op that still resets `last_touched_at = now` (so it doubles as a manual "touch" if a user wants).

**Success response (200):** updated item block.

**Error responses:**

| Code | When |
|---|---|
| 404 | note or item not found |

---

## Server-internal: the auto-archive cron

Not a public API surface. Documented here so it's clear what mutates state behind the scenes.

- **Cadence:** daily at 03:00 UTC.
- **Action:** `UPDATE checklist_items SET archived_at = now WHERE archived_at IS NULL AND (now − last_touched_at) >= 60 days`.
- **Affects:** both done and undone items.
- **Construction's choice:** scheduling mechanism (in-process coroutine loop, Quartz, Railway cron job, etc.). Not specified by Inception.

---

## Open contract questions

See [[open-questions]] — items Q2, Q3, Q4, Q5 above all flag fields/behaviors marked **TBD** in this document. Mob must resolve before this contract is "done".
