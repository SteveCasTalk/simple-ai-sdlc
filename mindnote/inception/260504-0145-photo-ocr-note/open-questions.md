---
type: open-questions
feature: photo-ocr-note
created: 2026-05-04
tags:
  - inception/open-questions
  - feature/photo-ocr-note
  - status/closed
---

# Open questions

> [!success] **Empty.** All open questions resolved by the mob on 2026-05-04. Inception phase definition-of-done now hinges on issue completion, not unresolved questions.

## Open

_(none)_

## Resolved

### Q1 — Should we add a product success metric beyond "it ships"? — 2026-05-04

- **Answer:** No product metric for v1. The feature is considered successful when the end-to-end test against a single fixture image passes (image upload → OCR → editable note → save).
- **By:** mob
- **Promoted to:** [[PRD#Success metrics]]

### Q2 — Confirm Google Cloud Vision over Tesseract self-hosted? — 2026-05-04

- **Answer:** Google Cloud Vision. D1 ratified.
- **By:** mob
- **Promoted to:** [[decisions]] D1

### Q3 — Confirm the body-marker shortcut for image attachment (D4)? — 2026-05-04

- **Answer:** **Reversed.** Add a proper `imagePath` field to the `Note` model on Android (Room migration). Server `Note` stays unchanged — image is local-only.
- **By:** mob
- **Promoted to:** [[decisions]] D4 (rewritten); new issue [[issues/android/06-imagepath-schema-migration]].
