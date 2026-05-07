---
type: prd
feature: photo-ocr-note
status: draft
created: 2026-05-04
tags:
  - inception/prd
  - feature/photo-ocr-note
  - status/draft
---

# PRD: Photo → OCR → Searchable note

> [!info] **Status:** Draft / awaiting mob review · **Driver:** steve · **Last updated:** 2026-05-04
> See [[_index]] for the parallel-work plan and [[open-questions]] for unresolved items.

## One-line intent

A student snaps or picks any photo containing text; the app sends it to the server, which OCRs it; the extracted text becomes a normal MindNote note (searchable like every other note) with the original photo kept as an attachment.

## Problem

Students lose time retyping content from photos they take of whiteboards, slides, handouts, and printed pages. Today MindNote has no way to capture image-borne text — they either type it manually or skip capturing it. We want to remove that friction so the act of "save this for later" is a tap, not a transcription session.

## Goals

Testable goals.

- [ ] A user can launch the scan flow from a FAB on the Home screen.
- [ ] A user can pick from gallery **or** take a photo with the camera.
- [ ] The server returns OCR'd text for a 25 MB image within the 60 s client timeout, including multilingual content.
- [ ] After OCR returns, the user reviews and edits the extracted text **before** the note is saved.
- [ ] The user can cancel an in-flight OCR call.
- [ ] On save, the result becomes a regular `Note` and shows up in the existing notes list / search.
- [ ] The original photo is retained alongside the saved note.

## Non-goals

Promoted as needed to [[out-of-scope]].

- Offline OCR / on-device fallback.
- Editing or re-running OCR on an already-saved note.
- Batching multiple images into one note (one photo → one note for v1).
- Server-side image storage (image stays on the Android device).
- A new "search OCR'd notes" surface — OCR'd notes use the existing notes search.
- iOS — there is no iOS lane in the project.

## User stories

### Story 1 — Snap a whiteboard photo

**As a** student in a lecture, **I want** to snap a photo of the whiteboard and turn it into a searchable note, **so that** I can later search for what was on the board without retyping it.

**Acceptance criteria:**
- [ ] From Home, tapping the Scan FAB opens the scan flow.
- [ ] I can choose "Take photo" or "Pick from gallery"; both reach the same review step.
- [ ] While OCR is running, I see a loading state with a Cancel button.
- [ ] If I tap Cancel, the in-flight request is aborted and I return to the previous step (no note is created).
- [ ] If the request takes longer than 60 s, the app auto-cancels and shows an error toast.
- [ ] On success, I see the extracted text in an editable field with the source photo previewed above it.
- [ ] On Save, a new note is created with the edited text as its body and the original photo attached locally.
- [ ] After Save, I'm taken to the new note's detail (or back to Home) and the note is visible in the notes list.

### Story 2 — Photo of a printed handout (multilingual)

**As a** student studying a non-English textbook page, **I want** the OCR to recognize multilingual text, **so that** I can capture pages in the language I'm studying.

**Acceptance criteria:**
- [ ] The server-side provider supports at least the languages listed in [[decisions]] D2.
- [ ] A photo of a non-English printed page returns the correct text (verified by integration test with a fixture image).

### Story 3 — Failure path

**As a** user with a bad image / network blip, **I want** clear feedback when OCR fails, **so that** I know to retry rather than wait forever.

**Acceptance criteria:**
- [ ] If the server returns an error envelope, the app shows an error toast with the server's message and stays on the picker step.
- [ ] If the network is down, the app shows a generic error toast (no offline queue, no auto-retry).
- [ ] If OCR returns empty text, the app shows an error toast and does **not** auto-create an empty note.

## Success metrics

- **Acceptance metric (mob-set, 2026-05-04):** the feature is successful when an end-to-end test against a single fixture image passes — gallery pick / camera capture → `POST /ocr` → editable Review screen with extracted text → Save → note visible in the notes list with the original photo retained on-device. One image, one green pass.

## Constraints

- **Free OCR provider only** — see [[decisions]] D1 for the chosen provider.
- **Max image size: 25 MB.**
- **Online required** — feature is server-side.
- **Multilingual** — provider must handle at least English + one major non-Latin script (see [[decisions]] D2).
- **No privacy/compliance work** required for v1 (driver confirmed no FERPA-style scope).
- No iOS work in scope (no iOS in this project).

## Links

- API contract: [[api-contract]]
- Context: [[CONTEXT]] (project root)
- Issues: `./issues/`
