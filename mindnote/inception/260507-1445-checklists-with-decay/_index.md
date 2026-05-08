---
type: feature-index
feature: checklists-with-decay
status: draft
created: 2026-05-07
tags:
  - inception/index
  - feature/checklists-with-decay
  - status/draft
---

# Checklists with decay — feature index

> [!info] **Status:** Draft / awaiting mob review
> Generated 2026-05-07. Update this file as issues land.

## Quick links

- PRD: [[PRD]]
- API contract: [[api-contract]]
- Decisions: [[decisions]]
- Open questions: [[open-questions]] _(5 unresolved — mob)_
- Out of scope: [[out-of-scope]]
- Project-wide context: [[../../CONTEXT|CONTEXT]]

---

## Parallel work plan

15 stories across 2 lanes (Backend, Android). Foundation in waves 0–1; the user-facing bulk lands in wave 2 (11 stories grabbable simultaneously across BE and Android).

> [!tip]
> Cross-lane dependencies are *implicit*: Android can develop against the api-contract using mocks. The waves below encode **within-lane** sequencing only. Once the api-contract is locked (zero TBDs), BE and Android run fully concurrently.

### 🟢 Wave 0 — start here (no blockers)

| Issue | Estimate | Lane |
|---|---|---|
| [[issues/backend/01-block-shaped-notes-foundation]] | 90m | Backend |
| [[issues/android/01-block-aware-domain-and-room]] | 90m | Android |

### 🟡 Wave 1 — unlocked once wave 0 lands

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/backend/02-create-checklist-item-endpoint]] | 45m | Backend | `BE-01` |
| [[issues/android/02-render-blocks-with-decay-opacity]] | 75m | Android | `AN-01` |

### 🟠 Wave 2 — the bulk of the user-facing feature (11 issues — fully parallelizable)

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/backend/03-update-checklist-item-endpoint]] | 60m | Backend | `BE-02` |
| [[issues/backend/04-delete-checklist-item-endpoint]] | 30m | Backend | `BE-02` |
| [[issues/backend/05-archived-count-and-include-archived]] | 45m | Backend | `BE-02` |
| [[issues/backend/06-auto-archive-cron]] | 60m | Backend | `BE-02` |
| [[issues/backend/07-rescue-checklist-item-endpoint]] | 30m | Backend | `BE-02` |
| [[issues/android/03-toggle-item-done]] | 45m | Android | `AN-02` |
| [[issues/android/04-edit-item-text]] | 60m | Android | `AN-02` |
| [[issues/android/05-create-item-via-shortcut-or-toolbar]] | 75m | Android | `AN-02` |
| [[issues/android/06-reorder-items-drag-handle]] | 60m | Android | `AN-02` |
| [[issues/android/07-delete-item-long-press]] | 30m | Android | `AN-02` |
| [[issues/android/08-archived-expander-and-rescue]] | 60m | Android | `AN-02` |

> **Total estimate:** ~855m (~14h 15m) of focused TDD work across the team. With 1 BE + 1 Android dev, wall-clock ≈ 7–9h once wave 0/1 land. With 2+ devs in either lane during wave 2, much faster — many wave-2 issues are independently grabbable within a lane.

---

## All issues

```
issues/
├── backend/
│   ├── 01-block-shaped-notes-foundation.md          (wave 0)
│   ├── 02-create-checklist-item-endpoint.md          (wave 1)
│   ├── 03-update-checklist-item-endpoint.md          (wave 2)
│   ├── 04-delete-checklist-item-endpoint.md          (wave 2)
│   ├── 05-archived-count-and-include-archived.md     (wave 2)
│   ├── 06-auto-archive-cron.md                        (wave 2)
│   └── 07-rescue-checklist-item-endpoint.md           (wave 2)
└── android/
    ├── 01-block-aware-domain-and-room.md              (wave 0)
    ├── 02-render-blocks-with-decay-opacity.md         (wave 1)
    ├── 03-toggle-item-done.md                         (wave 2)
    ├── 04-edit-item-text.md                           (wave 2)
    ├── 05-create-item-via-shortcut-or-toolbar.md      (wave 2)
    ├── 06-reorder-items-drag-handle.md                (wave 2)
    ├── 07-delete-item-long-press.md                   (wave 2)
    └── 08-archived-expander-and-rescue.md             (wave 2)
```

> [!note] **How to update this index as work lands**
> When an issue's status changes, update its frontmatter (`status: ready` → `in-progress` → `done`). Use Obsidian's search `path:issues tag:#status/ready` to see what's grabbable.

---

## Definition of done (whole feature)

- [ ] All 15 issues have status `done`.
- [ ] [[open-questions]] has zero unresolved items (5 currently — Q1–Q5).
- [ ] [[PRD]] has its 3 success metrics confirmed by the mob.
- [ ] [[api-contract]] has zero `TBD` markers (3 currently — Q2/Q3/Q4 each cite a contract TBD; Q5 is delete-idempotency).
- [ ] End-to-end on a real device: a user can create a note with mixed prose + items, toggle / edit / reorder / delete items, see decay opacity, see auto-archived items behind the expander after the cron runs, and rescue them.
