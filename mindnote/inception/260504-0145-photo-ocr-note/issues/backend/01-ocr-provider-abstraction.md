---
type: issue
feature: photo-ocr-note
lane: backend
status: ready
wave: 0
estimate: 60m
blocked-by: []
tags:
  - inception/issue
  - lane/backend
  - feature/photo-ocr-note
  - status/ready
  - wave/0
---

# [BE] Add `OcrProvider` interface + adapter for the chosen free provider

**Lane:** Backend
**PRD section:** Goals — multilingual provider; Constraints — free OCR provider only.
**API contract section:** N/A (consumed in [[02-ocr-route-and-multipart]]).

## Why

Hides the OCR engine behind a single Kotlin interface so the route can be written, tested, and reviewed independently of the vendor choice. If we have to swap providers (quota, quality, pricing), only this file changes.

## Implementation steps

1. Create `server/src/main/kotlin/com/mindnote/server/OcrProvider.kt` with:
   ```kotlin
   interface OcrProvider {
       suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult
   }
   data class OcrResult(val text: String, val languageHint: String)
   sealed class OcrException(msg: String) : RuntimeException(msg) {
       class Empty : OcrException("provider returned no text")
       class Upstream(val status: Int, msg: String) : OcrException(msg)
   }
   ```
2. Create the chosen adapter in the same file (or a sibling file) — implementation depends on [[../../decisions|D1]]. Default direction in this PRD: **Google Cloud Vision REST API** via Ktor `HttpClient` with the API key read from `OCR_API_KEY` env var.
3. Wire a singleton instance into `Application.kt` `Application.module()` (or a new `installOcr()` extension) so it can be injected into the route in issue 02.
4. If `OCR_API_KEY` is missing at startup, log a warning and install a `StubOcrProvider` that throws `OcrException.Upstream(503, "no provider configured")` — this lets the server still boot for unrelated dev work.
5. Add Gradle dependencies if needed (Ktor `HttpClient` is already present; double-check `server/build.gradle.kts`).

## Files to touch

- `server/src/main/kotlin/com/mindnote/server/OcrProvider.kt` — create.
- `server/src/main/kotlin/com/mindnote/server/Application.kt` — modify (install/inject provider).
- `server/build.gradle.kts` — modify only if a new dependency is required.

## Acceptance criteria

- [ ] `OcrProvider` interface and `OcrResult` / `OcrException` types compile and are public to the module.
- [ ] Concrete adapter for the chosen provider exists; missing API key falls back to `StubOcrProvider` without crashing the server.
- [ ] Unit test: a fake `OcrProvider` can be constructed in tests and substituted via DI/install hook (test in [[02-ocr-route-and-multipart]] uses this fake).
- [ ] `./gradlew :server:build` succeeds.

## Blocked by

- Nothing — independently grabbable.

## Notes

- Keep adapter implementation small. Multipart parsing belongs to the route, not the provider.
- Real provider call goes here — but the integration test in issue 02 will substitute a fake to keep CI offline-safe.
