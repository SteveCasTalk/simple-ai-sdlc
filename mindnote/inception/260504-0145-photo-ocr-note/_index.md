---
type: feature-index
feature: photo-ocr-note
status: ready
created: 2026-05-04
tags:
  - inception/index
  - feature/photo-ocr-note
  - status/ready
---

# Photo → OCR → Searchable note — feature index

> [!success] **Status:** Mob review complete (2026-05-04). All open questions resolved. Ready to grab.

## Quick links

- PRD: [[PRD]]
- API contract: [[api-contract]]
- Decisions: [[decisions]]
- Open questions: [[open-questions]] _(empty)_
- Out of scope: [[out-of-scope]]
- Project-wide context: [[../../CONTEXT|CONTEXT]]

---

## Parallel work plan

8 issues across 2 lanes (Backend, Android). All 5 wave-0 issues are grabbable now.

### 🟢 Wave 0 — start here (no blockers)

| Issue | Estimate | Lane |
|---|---|---|
| [[issues/backend/01-ocr-provider-abstraction]] | 60m | Backend |
| [[issues/android/01-image-pick-and-camera-deps]] | 30m | Android |
| [[issues/android/02-ocr-api-client]] | 45m | Android |
| [[issues/android/03-scan-fab-and-nav-scaffold]] | 45m | Android |
| [[issues/android/06-imagepath-schema-migration]] | 60m | Android |

### 🟡 Wave 1 — unlocked once wave 0 lands

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/backend/02-ocr-route-and-multipart]] | 75m | Backend | `BE-01` |
| [[issues/android/04-image-source-picker]] | 60m | Android | `A-01`, `A-03` |

### 🟠 Wave 2 — final integration

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/android/05-ocr-call-review-and-save]] | 90m | Android | `A-02`, `A-04`, `A-06`, `BE-02` |

---

## All issues

```
issues/
├── backend/
│   ├── 01-ocr-provider-abstraction.md
│   └── 02-ocr-route-and-multipart.md
└── android/
    ├── 01-image-pick-and-camera-deps.md
    ├── 02-ocr-api-client.md
    ├── 03-scan-fab-and-nav-scaffold.md
    ├── 04-image-source-picker.md
    ├── 05-ocr-call-review-and-save.md
    └── 06-imagepath-schema-migration.md
```

> [!note] **Parallelization sanity check**
> - Wave 0: **5 parallel issues** across both lanes. ✓
> - Wave 1: **2 parallel issues** across both lanes. ✓
> - Wave 2: 1 issue (final integration on Android — pulls together OCR client, picker, schema, and live server). Inevitable serialisation.
>
> **Key parallelism win:** because the API contract has zero `TBD`s, the BE route (`BE-02`) and the Android picker (`A-04`) progress in lockstep. The schema migration (`A-06`) is fully independent and can land first to de-risk the data-model change before integration.

> [!tip] **How to update this index as work lands**
> When an issue's status changes, update its frontmatter (`status: ready` → `in-progress` → `done`). Use Obsidian's search `path:issues tag:#status/ready` to see what's grabbable.

---

## Definition of done (whole feature)

- [ ] All 8 issues have status `done` in their frontmatter.
- [x] [[open-questions]] has zero unresolved items.
- [x] [[PRD]] has at least one success metric.
- [x] [[api-contract]] has zero `TBD` markers.
- [x] [[decisions]] has been ratified by the mob.
- [ ] End-to-end (mob-set acceptance test): a single fixture image successfully flows through pick → OCR → review → save → appears in notes list with `imagePath` populated and the file present on disk.
