---
type: api-contract
feature: photo-ocr-note
created: 2026-05-04
backend-work: true
tags:
  - inception/api-contract
  - feature/photo-ocr-note
---

# API contract

> [!success] **No `TBD`s remaining** — BE / Android can proceed in parallel.

## Conventions

- **Auth:** `X-Device-Id` header, matching the existing `notesRoutes` convention in `server/Routes.kt`. Missing/blank header falls back to `LOCAL_USER_ID` (single-device demo).
- **Base URL:** Same as existing endpoints (configured per environment; see `server/Application.kt`).
- **Error envelope:** `{ "error": { "code": string, "message": string } }`. Codes used by this feature: `image_too_large`, `unsupported_media`, `ocr_empty`, `ocr_provider_error`, `bad_request`.
- **Date format:** ISO 8601 UTC (no dates in this contract; noted for consistency).

## Endpoints

### `POST /ocr` — Run OCR on an uploaded image and return extracted text.

**Auth:** required (`X-Device-Id`).

**Request:** `multipart/form-data` with a single part:

| Part name | Type | Required | Notes |
|---|---|---|---|
| `image` | file | yes | JPEG, PNG, or WebP. Max body size **25 MB** (enforced by server with `413 Payload Too Large` if exceeded). |

> [!info] **Why multipart, not base64 JSON?** Avoids the ~33% base64 overhead and works cleanly with Android's `MultipartBody.Part` and Ktor's `receiveMultipart()`.

**Success response (200):**

```json
{
  "text": "string",
  "languageHint": "string"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `text` | string | yes | Extracted text. Whitespace and line breaks preserved as the provider returns them. May contain non-Latin scripts. |
| `languageHint` | string | no | Best-guess BCP-47 language tag from the provider, if available (e.g., `en`, `vi`, `ja`). Empty string if the provider doesn't offer one. |

> [!warning] **Empty text is an error, not a 200.** If the OCR provider returns no text or only whitespace, the server responds **422** with `code: "ocr_empty"` so the client can show the right toast and not auto-save an empty note.

**Error responses:**

| HTTP | Code | When |
|---|---|---|
| 400 | `bad_request` | Multipart missing `image` part, or part has no filename. |
| 415 | `unsupported_media` | Content-type is not JPEG / PNG / WebP. |
| 413 | `image_too_large` | Image body exceeds 25 MB. |
| 422 | `ocr_empty` | Provider returned empty / whitespace-only text. |
| 502 | `ocr_provider_error` | Upstream OCR provider returned an error. `message` includes the provider's status if safe to expose. |

All error bodies match the standard envelope.

**Notes:**
- **Idempotency:** None. Each call re-runs OCR; client de-dup is unnecessary because the user controls when to trigger.
- **Pagination:** N/A.
- **Rate limits:** None enforced server-side for v1; provider's free-tier quota is the practical limit (see [[decisions]] D1).
- **Server-side persistence:** None. The image is read into memory, sent to the provider, and discarded. Only the resulting note (text-only) is later saved by the existing `POST /notes`.
- **Timeout:** Server should call the provider with a budget under the client's 60 s timeout — recommend 50 s upstream call timeout, fail with `502 ocr_provider_error` if exceeded.
