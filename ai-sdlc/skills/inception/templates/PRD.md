---
type: prd
feature: <feature-slug>
status: draft
created: <YYYY-MM-DD>
tags:
  - inception/prd
  - feature/<feature-slug>
  - status/draft
---

# PRD: <feature name>

> [!info] **Status:** Draft / In mob review / Approved · **Driver:** <name> · **Last updated:** <YYYY-MM-DD>
> See [[_index]] for the parallel-work plan and [[open-questions]] for unresolved items.

## One-line intent

<single sentence — what are we building?>

## Problem

<who has this problem, and what does it cost them today?>

## Goals

Testable goals. If you can't measure it, rewrite it.

- [ ] <goal 1>
- [ ] <goal 2>

## Non-goals

What we are explicitly NOT doing in this feature. Promote anything controversial to `out-of-scope.md`.

- <non-goal 1>

## User stories

Each story has acceptance criteria. A story without acceptance criteria is not ready to cut into issues.

### Story 1 — <short name>

**As a** <role>, **I want** <action>, **so that** <outcome>.

**Acceptance criteria:**
- [ ] <observable behavior 1>
- [ ] <observable behavior 2>

### Story 2 — <short name>

...

## Success metrics

How we know this feature worked, after launch. At least one.

- <metric — target — how measured>

## Constraints

Deadlines, dependencies, compliance, existing systems we must respect.

- <constraint>

## Links

- API contract: `./api-contract.md`
- Context: `./CONTEXT.md`
- Issues: `./issues/`
