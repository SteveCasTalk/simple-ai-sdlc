---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 0
estimate: 45m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/0
---

# [Android] Add `OcrApi` (Ktor multipart) + DTO + Koin registration

**Lane:** Android
**PRD section:** Story 1.
**API contract section:** `POST /ocr`.

## Why

Lets the ViewModel in later issues call the server without anyone touching network code again. Mirrors the existing Ktor pattern in [`data/remote/NotesApi.kt`](app/src/main/java/com/mindnote/data/remote/NotesApi.kt) — same constructor injection of `HttpClient`, same `client.post(...).body()` shape.

> [!important] **Stack:** mindnote uses **Ktor 3.0.3** (OkHttp engine) and **kotlinx.serialization** — see [CLAUDE.md](CLAUDE.md). Do NOT introduce Retrofit/Moshi.

## Implementation steps

1. Create `app/src/main/java/com/mindnote/data/remote/OcrApi.kt`. Mirror `NotesApi`'s shape (constructor takes the shared `HttpClient`):
   ```kotlin
   package com.mindnote.data.remote

   import io.ktor.client.HttpClient
   import io.ktor.client.call.body
   import io.ktor.client.request.forms.MultiPartFormDataContent
   import io.ktor.client.request.forms.formData
   import io.ktor.client.request.header
   import io.ktor.client.request.post
   import io.ktor.client.request.setBody
   import io.ktor.http.Headers
   import io.ktor.http.HttpHeaders

   class OcrApi(private val client: HttpClient) {
       suspend fun ocr(
           deviceId: String,
           imageBytes: ByteArray,
           contentType: String = "image/jpeg",
       ): OcrResponseDto =
           client.post("ocr") {
               header("X-Device-Id", deviceId)
               setBody(MultiPartFormDataContent(formData {
                   append("image", imageBytes, Headers.build {
                       append(HttpHeaders.ContentType, contentType)
                       append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                   })
               }))
           }.body()
   }
   ```
2. Add the DTO. Co-locate next to the API like `NotesApi`'s DTOs do (either in `OcrApi.kt` or a sibling `OcrDto.kt`):
   ```kotlin
   import kotlinx.serialization.SerialName
   import kotlinx.serialization.Serializable

   @Serializable
   data class OcrResponseDto(
       val text: String,
       @SerialName("language_hint") val languageHint: String = "",
   )
   ```
3. Register `OcrApi` in [`core/di/AppModule.kt`](app/src/main/java/com/mindnote/core/di/AppModule.kt) alongside the existing `NotesApi` / `ChatApi` lines:
   ```kotlin
   single { OcrApi(get()) }
   ```
   No changes to the `HttpClient` block — the existing client already has `ContentNegotiation` + `kotlinx.serialization` configured.
4. Add a DTO round-trip test at `app/src/test/java/com/mindnote/data/remote/OcrDtoTest.kt` (use the project's existing `Json` configuration; mirror whatever DTO test exists today, e.g. `DtoMappingTest`):
   ```kotlin
   val json = Json { ignoreUnknownKeys = true }
   val parsed = json.decodeFromString<OcrResponseDto>("""{"text":"hello","language_hint":"en"}""")
   assertEquals("hello", parsed.text)
   assertEquals("en", parsed.languageHint)
   ```

## Files to touch

- `app/src/main/java/com/mindnote/data/remote/OcrApi.kt` — create.
- `app/src/main/java/com/mindnote/core/di/AppModule.kt` — modify (one `single { OcrApi(get()) }` line).
- `app/src/test/java/com/mindnote/data/remote/OcrDtoTest.kt` — create.

## Acceptance criteria

- [ ] `OcrApi` compiles and is injectable via Koin (verify by `get<OcrApi>()` in any existing module).
- [ ] Unit test: JSON `{"text":"hello","language_hint":"en"}` deserializes to `OcrResponseDto("hello","en")`.
- [ ] `./gradlew :app:testDebugUnitTest` passes.
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] No new dependency added to `gradle/libs.versions.toml` or `app/build.gradle.kts`.

## Blocked by

- Nothing — independently grabbable. (Contract has zero `TBD`s.)

## Notes

- The shared `HttpClient` in `AppModule.kt` already has the base URL, `defaultRequest`, JSON content negotiation, and the `X-Device-Id` default header configured — `OcrApi` only adds the per-request multipart body and the per-call `X-Device-Id` override (for tests).
- Don't call this client yet; the screen wires it up in issue 05.
