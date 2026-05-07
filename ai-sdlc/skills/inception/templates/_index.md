---
type: feature-index
feature: <feature-slug>
status: draft
created: <YYYY-MM-DD>
tags:
  - inception/index
  - feature/<feature-slug>
  - status/draft
---

# <Feature title> — feature index

> [!info] **Status:** Draft / awaiting mob review
> Generated from Inception phase. Update this file as issues land.

## Quick links

- PRD: [[PRD]]
- API contract: [[api-contract]]
- Decisions: [[decisions]]
- Open questions: [[open-questions]]
- Out of scope: [[out-of-scope]]
- Project-wide context: [[CONTEXT]]

---

## Parallel work plan

Issues are grouped into **waves** by dependency depth. All issues in a wave can be picked up simultaneously by different devs.

> [!tip]
> Compute wave for each issue: 0 if `blocked-by` is empty, otherwise `1 + max(wave of each blocker)`.

### 🟢 Wave 0 — start here (no blockers)

| Issue | Estimate | Lane |
|---|---|---|
| [[NN-issue-slug]] | <Xm> | <lane> |

### 🟡 Wave 1 — unlocked once wave 0 lands

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[NN-issue-slug]] | <Xm> | <lane> | `NN`, `NN` |

### 🟠 Wave 2

…

### 🔵 Wave 3

…

### 🟣 Wave 4 — final integration

…

---

## All issues

```
issues/
├── backend/   (only if BE involved)
├── android/   (only if Android in project)
└── ios/       (only if iOS in project)
```

> [!note] **How to update this index as work lands**
> When an issue's status changes, update its frontmatter (`status: ready` → `in-progress` → `done`). The Properties panel and tag pane will reflect it. Use Obsidian's search `path:issues tag:#status/ready` to see what's grabbable.

---

## Definition of done (whole feature)

- [ ] All issues have status `done` in their frontmatter.
- [ ] [[open-questions]] has zero unresolved items.
- [ ] [[PRD]] has at least one success metric.
- [ ] If applicable, [[api-contract]] has zero `TBD` markers.
- [ ] End-to-end flow runs on a real device / staging.
