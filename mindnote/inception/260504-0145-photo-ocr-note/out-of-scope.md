---
type: out-of-scope
feature: photo-ocr-note
created: 2026-05-04
tags:
  - inception/out-of-scope
  - feature/photo-ocr-note
---

# Out of scope

> [!warning]
> Things this feature is explicitly **not** doing in v1.

- **Offline OCR / on-device fallback** — the entire flow is online; if the network is down, the user sees an error toast. Revisit if reliability complaints surface.
- **Re-running OCR on a saved note** — once saved, the note is just a note. Want to re-OCR? Take another photo.
- **Editing the attached image** — no crop, rotate, or filter. Camera/gallery output is final.
- **Multiple images per note** — one photo → one note for v1. Multi-image notes can be a separate feature.
- **Server-side image storage** — image is local-only on Android. The server only receives the bytes transiently, OCRs, and discards.
- **Sync of the attached image across devices** — naturally falls out of "no server-side image storage". A new device sees the OCR'd note text but not the original photo.
- **A new "search OCR'd notes" surface** — OCR'd notes use the existing notes search.
- **Schema changes on the server** — Android `NoteEntity` and domain `Note` get a new `imagePath: String?` field (see [[decisions]] D4). Server `Note` / `Notes` table / `NoteDto` stay unchanged because images are local-only.
- **iOS** — no iOS lane in this project; not creating an `issues/ios/` folder.
- **OCR provider quota / billing dashboards** — we'll find out we hit the free-tier ceiling when calls start failing. Acceptable for demo.
- **Refining Home FAB layout** — D3 adds a second FAB next to Capture. Visual polish on small screens is a designer pass for a future feature.
- **Privacy/compliance posture** — driver explicitly waived FERPA-style scope for v1.
