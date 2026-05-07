---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 2
estimate: 90m
blocked-by:
  - "[[02-ocr-api-client]]"
  - "[[04-image-source-picker]]"
  - "[[06-imagepath-schema-migration]]"
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/2
---

# [Android] OCR call (with cancel + 60 s timeout), Review state, and Save → note + local image

**Lane:** Android
**PRD section:** Story 1 (full happy path), Story 3 (failure handling).
**API contract section:** `POST /ocr`.

## Why

Closes the loop. After this lands, a user can scan a photo end-to-end: pick image → OCR returns → review and edit text → save → see a real searchable note in the list with the original photo retained on-device.

## Implementation steps

1. Extend `ScanContract.kt`:
   - Add `phase` variants: `Loading(val job: Job)`, `Review(val uri: Uri, val text: String, val languageHint: String)`, `Saving`.
   - Add intents: `RunOcr`, `Cancel`, `EditText(value: String)`, `Save`.
   - Add effects: `Saved(noteId: String)` (consumed by the existing `onSaved` callback in `MindNoteNavHost`).
2. In `ScanViewModel.kt`:
   - Inject `OcrApi`, `NotesRepository`, `Application` (for `contentResolver` + `filesDir`), and the existing device-id provider used by `NotesApi`.
   - On `RunOcr`: launch a coroutine in `viewModelScope` wrapped in `withTimeout(60_000)`; read the picked `Uri` as bytes — `contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("…")`. Resolve the content type with `contentResolver.getType(uri) ?: "image/jpeg"`. Call `api.ocr(deviceId, bytes, contentType)`. Hold the `Job` in `Loading` so `Cancel` can `job.cancel()`. (`OcrApi` already constructs the Ktor `MultiPartFormDataContent` internally — the VM just hands it bytes.)
   - On success with non-blank `text`: set `phase = Review(uri, text, languageHint)`.
   - On `Cancel`: `job.cancel()`, transition back to `Picked(uri)`.
   - On `TimeoutCancellationException` or non-2xx: `phase = Picked(uri)` and emit `ShowError` with provider's `message` if it's an HTTP error envelope, else a generic copy.
   - On `Save`:
      - Generate `noteId = "n-${System.currentTimeMillis()}"`.
      - Copy the image bytes from the picked `Uri` into `filesDir/ocr/<noteId>.jpg` (create dir if missing). Hold the absolute path as `imagePath`.
      - Build a `Note` with `id = noteId`, `title` = first non-empty line of edited text (or "Scanned note"), `body` = edited text, `preview` = first 120 chars, `tags = []`, `date = LocalDate.now()`, **`imagePath = imagePath`** (the field added in [[06-imagepath-schema-migration]]).
      - Call `notesRepository.save(note)` (mirror how `CaptureViewModel` does it).
      - Emit `ScanEffect.Saved(note.id)`.
3. In `ScanScreen.kt`:
   - Render `Loading`: full-screen `CircularProgressIndicator` + a Cancel button that sends `ScanIntent.Cancel`.
   - Render `Review`: `Coil AsyncImage` at the top (small thumbnail), an editable `BasicTextField` bound to `state.text` below it (sends `EditText`), and Save/Cancel buttons in the top bar (Cancel returns to `Picked`).
   - Render `Saving`: disable Save, show inline spinner (mirrors `CaptureScreen`'s top-bar pattern).
4. Wire the `Saved(id)` effect through `MindNoteNavHost` to navigate to `Routes.noteDetail(id)` (already plumbed in issue 03).

## Files to touch

- `app/src/main/java/com/mindnote/features/scan/ScanContract.kt` — modify.
- `app/src/main/java/com/mindnote/features/scan/ScanViewModel.kt` — modify.
- `app/src/main/java/com/mindnote/features/scan/ScanScreen.kt` — modify.
- `app/src/main/java/com/mindnote/core/di/AppModule.kt` — modify (add `OcrApi`, `Application` to VM constructor).
- `app/src/main/res/values/strings.xml` — modify (loading copy, save, errors).
- `app/src/test/java/com/mindnote/features/scan/ScanViewModelTest.kt` — create.

## Acceptance criteria

- [ ] Happy path: pick image → OCR call returns → editable review screen shows extracted text + image preview → Save → note appears in `Notes` and is openable from `NoteDetail`.
- [ ] Cancel during `Loading` aborts the request and returns to the picker step.
- [ ] OCR call exceeding 60 s auto-cancels and shows an error toast.
- [ ] Empty / `422 ocr_empty` server response shows an error toast and stays at the picker step (no empty note created).
- [ ] Network error (no connectivity) shows a generic error toast.
- [ ] The saved note's `imagePath` points at the persisted file under `filesDir/ocr/<noteId>.jpg`.
- [ ] The original image file persists at `filesDir/ocr/<noteId>.jpg` after save and survives an app restart.
- [ ] Unit tests: `ScanViewModelTest` covers (a) `RunOcr` happy path with fake `OcrApi`; (b) `Cancel` aborts a slow fake; (c) timeout produces `ShowError`; (d) empty text produces `ShowError`; (e) `Save` writes a `Note` with `imagePath` set through `NotesRepository` and emits `Saved`.
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` succeeds.
- [ ] Manual smoke: end-to-end on a device with the running server and a real image.

## Blocked by

- [[02-ocr-api-client]]
- [[04-image-source-picker]]
- [[06-imagepath-schema-migration]]

## Notes

- mindnote uses **Ktor** (not Retrofit). Per [CLAUDE.md](CLAUDE.md), API classes follow the `NotesApi` shape — `class XxxApi(private val client: HttpClient)` with `client.post(...).body()`. The shared `HttpClient` in `AppModule.kt` is already configured with `HttpTimeout` — but `withTimeout(60_000)` at the coroutine level is still required so `Cancel` cleanly cuts the job.
- For `NotesRepository.save`, follow whatever signature `CaptureViewModel` uses today (it persists locally via Room and pushes to the server). Verify before coding. The server doesn't know about `imagePath` — only the local Room write needs it.
