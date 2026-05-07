---
type: decisions
feature: photo-ocr-note
created: 2026-05-04
tags:
  - inception/decisions
  - feature/photo-ocr-note
---

# Decisions

> [!info]
> ADR-lite log. Driver-made decisions awaiting mob ratification.

---

### D1 — OCR provider: Google Cloud Vision (free tier) — 2026-05-04 — **RATIFIED**

- **Context:** Driver asked for "any free provider" with multilingual support and 25 MB max image. Two viable candidates surfaced.
- **Options considered:**
  - **Tesseract self-hosted** (e.g. tess4j or sidecar binary) — truly free, no quota, multilingual via language packs, **mediocre on handwriting** (whiteboards / student notes).
  - **Google Cloud Vision REST API** — free tier 1,000 requests/month, very strong on printed and handwritten text, multilingual out of the box.
  - **OCR.space** — has a free tier, lower quality, weaker non-Latin scripts.
- **Decision:** Default to **Google Cloud Vision**. Read API key from env var `OCR_API_KEY`; if unset, fall back to a stub that fails fast with `502 ocr_provider_error`.
- **Why:** Quality dominates: target users are students photographing whiteboards and handwritten notes, where Tesseract underperforms badly. The free tier (1k/month) covers the demo and early dogfood. The provider abstraction in [[issues/backend/01-ocr-provider-abstraction]] keeps the door open to swap to Tesseract later if quota becomes a problem.
- **Consequences:** Hard ceiling at 1k OCR calls/month/project. Once that's hit, server returns `502` until the next month or until the provider is swapped. No PII concerns — driver confirmed no privacy/compliance scope.

### D2 — Multilingual scope: any language the chosen provider supports — 2026-05-04

- **Context:** Driver said "multilingual" without naming languages. Vision API supports ~60 languages auto-detected; we don't want to send a `language` hint that would constrain it.
- **Options considered:** Hard-code an allow-list (e.g. en + vi + ja); auto-detect with no constraint.
- **Decision:** Auto-detect — send no language hint. Server returns the provider's `languageHint` in the response so the client can show it later.
- **Why:** Students may capture content in unpredictable languages; constraining the provider would degrade quality for no win.
- **Consequences:** Quality varies by language. Acceptance test should exercise at least English + one non-Latin script.

### D3 — Home Scan FAB sits **alongside** the existing Capture FAB, not replacing it — 2026-05-04

- **Context:** PRD says "FAB on Home screen". Home already has a centered accent FAB for Capture (manual note entry). Three options.
- **Options considered:**
  - **Replace** the Capture FAB with a Scan FAB (loses manual-entry entry point on Home).
  - **Replace** with a multi-action FAB that opens a sheet ("Type" / "Scan").
  - **Add** a second, smaller Scan FAB next to the existing one.
- **Decision:** Add a second, smaller Scan FAB to the right of the existing Capture FAB.
- **Why:** Cheapest and most reversible. Doesn't regress manual capture. Multi-action sheet is more polished but adds a tap.
- **Consequences:** Home gets visually busier. If layout becomes ugly on small screens, designer can revisit; flagged in [[out-of-scope]].

### D4 — Image attachment via Android-only `imagePath` field on `Note` — 2026-05-04 — **REWRITTEN BY MOB**

> [!warning] **Reversed on 2026-05-04 (mob review).** Original draft chose a body-marker shortcut to avoid a schema change; the mob preferred a proper field. This entry now reflects the new direction.

- **Context:** Driver asked to keep the original photo attached to the saved note. The `Note` model has no image field. Mob ruled that the data model must reflect the attachment cleanly.
- **Options considered:**
  - **Schema change, Android-only**: add `imagePath: String?` to `NoteEntity` and the domain `Note`. Server `Note` stays unchanged because images are local-only (per [[out-of-scope]]).
  - **Schema change, both sides**: add `imagePath` to server `Notes` table + `NoteDto`. Rejected — the server would store a meaningless device-local path.
  - **Body marker** (original draft): append `\n[image:<path>]` to `body` and strip for preview. Rejected by mob — pollutes `body` and search.
- **Decision:** Schema change on Android only. Add `NoteEntity.imagePath: String?` (nullable, null for the existing manual-capture path). Bump Room DB version 1 → 2 with a real migration that adds the column. Domain `Note` gets `imagePath: String? = null`. Server stays unchanged.
- **Why:** Cleaner data model. Search no longer false-matches an internal marker. The "merge with server DTO" concern is solved by having `NoteMapping` preserve the local `imagePath` when refreshing from the DTO (DTO has no `imagePath`, so the local value wins).
- **Consequences:** Adds one issue ([[issues/android/06-imagepath-schema-migration]]) on the critical path. New devices that re-pull notes from the server lose the local photo (acceptable — already in [[out-of-scope]] as "no cross-device image sync"). DB migration must be a real `Migration(1,2)`, not destructive — protects any existing test/dev data.

### D5 — OCR cancel + 60 s timeout semantics — 2026-05-04

- **Context:** Driver said "yes they can cancel after 1 min". Two readings.
- **Options considered:** Cancel button visible only after 60 s; Cancel always visible + 60 s auto-cancel.
- **Decision:** Cancel button is visible the whole time the request is in flight; the client also auto-cancels at 60 s and shows an error toast.
- **Why:** Lower-friction UX — user can bail at any point. Auto-cancel ensures the spinner doesn't run forever on a stuck network.
- **Consequences:** Slightly busier UI during loading. Server should call the provider with a 50 s upstream timeout to leave headroom.

### D6 — On any failure, toast and stay; no offline queue — 2026-05-04

- **Context:** Driver said "if failed, simply toast error". Includes network failure.
- **Decision:** All failures (network, 4xx, 5xx, empty OCR result) → toast + stay at the picker step. No retry queue, no automatic retry.
- **Why:** Simplest possible v1. Online-only is already a hard constraint.
- **Consequences:** Power users on flaky networks will retry manually. Acceptable for student demo scope.
