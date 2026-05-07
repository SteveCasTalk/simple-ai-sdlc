---
type: issue
feature: photo-ocr-note
lane: backend
status: ready
wave: 1
estimate: 75m
blocked-by:
  - "[[01-ocr-provider-abstraction]]"
tags:
  - inception/issue
  - lane/backend
  - feature/photo-ocr-note
  - status/ready
  - wave/1
---

# [BE] Add `POST /ocr` route — multipart upload, provider call, error envelope

**Lane:** Backend
**PRD section:** Story 1, Story 3.
**API contract section:** `POST /ocr` (the only endpoint in the contract).

## Why

This is the endpoint the Android scan flow calls. It owns the contract: image-size validation, content-type validation, calling the provider, mapping provider exceptions to the documented error envelope, and the 50 s upstream timeout.

## Implementation steps

1. In `server/src/main/kotlin/com/mindnote/server/Routes.kt` (or a new `OcrRoutes.kt`), add `fun Route.ocrRoutes(provider: OcrProvider)`.
2. Inside, define `post("/ocr") { ... }`:
   1. Read `X-Device-Id` via the existing `userIdFromHeader()` helper. (Auth alignment with notes/favorites routes — the user is implicitly trusted; we just want a consistent header.)
   2. Receive `call.receiveMultipart()`. Iterate parts; pick the first `PartData.FileItem` named `image`.
   3. If no such part: respond `400` with envelope `{error: {code: "bad_request", message: "missing image part"}}`.
   4. Validate `contentType` against `["image/jpeg", "image/png", "image/webp"]`. If not in the allow-list: `415 unsupported_media`.
   5. Stream bytes; if total exceeds `25 * 1024 * 1024`: `413 image_too_large`. (Use Ktor's `requestLimit` or check after read.)
   6. Call `withTimeout(50_000) { provider.extractText(bytes, contentType) }`.
   7. On success, if `text.isBlank()`: `422 ocr_empty`.
   8. On `OcrException.Upstream` or `TimeoutCancellationException`: `502 ocr_provider_error` with provider's message (sanitised).
   9. Otherwise: respond `200` with `OcrResponseDto(text, languageHint)`.
3. Add `OcrResponseDto` (and `ErrorDto` if not already present) to `server/src/main/kotlin/com/mindnote/server/Dto.kt`. Keep them `@Serializable`.
4. Wire `ocrRoutes(provider)` into `Application.module()` alongside `notesRoutes()` and `favoritesRoutes()`.

## Files to touch

- `server/src/main/kotlin/com/mindnote/server/Routes.kt` (or new `OcrRoutes.kt`) — create route.
- `server/src/main/kotlin/com/mindnote/server/Dto.kt` — add `OcrResponseDto`, `ErrorDto`/`ErrorEnvelope` if absent.
- `server/src/main/kotlin/com/mindnote/server/Application.kt` — register route.

## Acceptance criteria

- [ ] Endpoint `POST /ocr` returns the response shape defined in [[../../api-contract]].
- [ ] Error envelope matches the contract for `400 / 413 / 415 / 422 / 502`.
- [ ] Empty provider text is mapped to `422 ocr_empty` — never returned as a 200.
- [ ] Upstream timeout (50 s) maps to `502 ocr_provider_error`.
- [ ] Integration test: a Ktor `testApplication` POSTs a small fixture JPEG to `/ocr` with a fake `OcrProvider` and asserts 200 + body shape; a second test POSTs an oversized payload and asserts 413; a third uses a fake that throws `OcrException.Empty` and asserts 422. Test file: `server/src/test/kotlin/com/mindnote/server/OcrRoutesTest.kt`.
- [ ] `./gradlew :server:test :server:build` succeeds.

## Blocked by

- [[01-ocr-provider-abstraction]]

## Notes

- Keep the route thin — anything provider-specific should already be inside the adapter from issue 01.
- Don't enforce `Content-Length` upfront; Ktor lets you stream. A streaming check is fine, but a simple "read into ByteArray with size cap" is also acceptable.
