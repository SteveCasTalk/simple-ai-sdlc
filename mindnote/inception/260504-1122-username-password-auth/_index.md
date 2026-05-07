---
type: feature-index
feature: username-password-auth
status: draft
created: 2026-05-04
tags:
  - inception/index
  - feature/username-password-auth
  - status/draft
---

# Username + password authentication — feature index

> [!warning] **Status:** Draft. Inception draft produced 2026-05-04. Awaiting mob review of [open-questions](open-questions.md) and ratification of [decisions](decisions.md).

## Quick links

- PRD: [[PRD]]
- API contract: [[api-contract]]
- Decisions (ratify): [[decisions]]
- Open questions (mob): [[open-questions]]
- Out of scope: [[out-of-scope]]
- Project-wide context: [[../../CONTEXT|CONTEXT]]
- Project agent guides: [[../../CLAUDE|CLAUDE]] (Android), [[../../server/CLAUDE|server/CLAUDE]] (Backend)

---

## Parallel work plan

**13 stories across 2 lanes** (Backend, Android). 3 wave-0 stories grabbable now.

### 🟢 Wave 0 — start here (no blockers)

| Issue | Estimate | Lane |
|---|---|---|
| [[issues/backend/01-account-and-token-schema]] | 60m | Backend |
| [[issues/android/01-token-storage-helper]] | 30m | Android |
| [[issues/android/02-auth-api-client]] | 45m | Android |

### 🟡 Wave 1 — unlocked once wave 0 lands

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/backend/02-register-endpoint]] | 60m | Backend | `BE-01` |
| [[issues/backend/03-login-endpoint]] | 45m | Backend | `BE-01` |
| [[issues/backend/04-logout-endpoint]] | 30m | Backend | `BE-01` |
| [[issues/backend/05-bearer-auth-on-existing-routes]] | 60m | Backend | `BE-01` |
| [[issues/android/03-http-client-attaches-bearer]] | 30m | Android | `A-01` |
| [[issues/android/04-app-entry-routing]] | 45m | Android | `A-01` |

### 🟠 Wave 2 — final integration (all Android)

| Issue | Estimate | Lane | Blocked by |
|---|---|---|---|
| [[issues/android/05-login-screen-and-flow]] | 75m | Android | `A-01`, `A-02`, `A-04` |
| [[issues/android/06-register-screen-and-flow]] | 90m | Android | `A-01`, `A-02`, `A-04` |
| [[issues/android/07-logout-flow]] | 30m | Android | `A-01`, `A-02`, `A-03` |
| [[issues/android/08-401-interceptor]] | 45m | Android | `A-01`, `A-03` |

---

## All stories

```
issues/
├── backend/
│   ├── 01-account-and-token-schema.md
│   ├── 02-register-endpoint.md
│   ├── 03-login-endpoint.md
│   ├── 04-logout-endpoint.md
│   └── 05-bearer-auth-on-existing-routes.md
└── android/
    ├── 01-token-storage-helper.md
    ├── 02-auth-api-client.md
    ├── 03-http-client-attaches-bearer.md
    ├── 04-app-entry-routing.md
    ├── 05-login-screen-and-flow.md
    ├── 06-register-screen-and-flow.md
    ├── 07-logout-flow.md
    └── 08-401-interceptor.md
```

> [!note] **Parallelization sanity check**
> - **Wave 0:** 3 parallel stories across 2 lanes (1 BE + 2 Android). ✓
> - **Wave 1:** 6 parallel stories (4 BE + 2 Android). ✓ — strongest fan-out point.
> - **Wave 2:** 4 parallel stories, all Android. Backend lane is done after wave 1; that team can pick up the next feature. ✓ within-lane.
>
> **Total estimate:** ~675m (~11 h) of focused TDD work across the team. With 2 devs (1 BE + 1 Android), wall-clock ≈ 6-7h if they run continuously. With more devs in either lane, faster.

> [!tip] **How to update this index as work lands**
> Status of each story is tracked by GitHub issue labels (managed by the orchestration-mcp). Use `list_features` / `list_ready_issues` for the live view rather than this static file. This file is the design-time wave plan; live state is on GitHub.

---

## Definition of done (whole feature)

- [ ] All 13 stories have status `done` (closed PRs).
- [ ] [[open-questions]] resolved (or explicitly deferred by mob).
- [ ] [[PRD]] success metrics: e2e test passes (register → notes call → logout → 401).
- [ ] [[api-contract]] still has zero `TBD` markers.
- [ ] [[decisions]] ratified by the mob.
- [ ] **Manual smoke on a real device:** register a fresh account → take some notes → logout → log back in → notes appear → 401 path tested by manually deleting the token row server-side and confirming the app routes to Login.
- [ ] **Server-side data wipe per Decision D5** has been run (one-time bootstrap step in production).
