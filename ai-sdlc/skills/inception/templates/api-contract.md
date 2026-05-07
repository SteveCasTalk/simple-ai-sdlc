---
type: api-contract
feature: <feature-slug>
created: <YYYY-MM-DD>
backend-work: true
tags:
  - inception/api-contract
  - feature/<feature-slug>
---

# API contract

> [!warning] **Keystone artifact.**
> When this file has zero `TBD` markers, BE / Android / iOS can proceed in parallel.

If this feature has no BE work, replace this entire file with a callout:

```
> [!success] **No backend changes** — feature is purely client-side.
```

And set `backend-work: false` in the frontmatter.

## Conventions

- **Auth:** <Bearer token / cookie / etc.>
- **Base URL:** <e.g. https://api.example.com/v1>
- **Error envelope:** all errors return `{ "error": { "code": string, "message": string, "details": object? } }`
- **Date format:** ISO 8601 UTC

## Endpoints

### `<METHOD> <path>` — <one-line purpose>

**Auth:** required / public

**Request:**

```json
{
  "field": "TBD"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| field | string | yes | TBD — see open-questions.md |

**Success response (200):**

```json
{
  "id": "string"
}
```

**Error responses:**

| Code | When | Message shape |
|---|---|---|
| 400 | invalid input | standard error envelope |
| 401 | missing/expired token | standard error envelope |

**Notes:**
- Idempotency: <yes / no / via key>
- Pagination: <cursor / page / none>
- Rate limits: <if any>

---

### `<METHOD> <path>` — <next endpoint>

...
