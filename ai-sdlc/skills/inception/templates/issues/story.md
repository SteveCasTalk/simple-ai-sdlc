---
type: issue
feature: <feature-slug>
lane: <android|ios|backend|web|...>
status: ready
wave: <0..N>
estimate: <Xm>     # rough order-of-magnitude only — Construction sizes the real work
blocked-by: []     # or list of "[[NN-other-story]]" wikilinks (same lane)
tags:
  - inception/issue
  - lane/<lane>
  - feature/<feature-slug>
  - status/ready
  - wave/<0..N>
---

# [<Lane>] <one-line story title — what user-observable outcome this delivers>

**Lane:** <Android | iOS | Backend | …>
**PRD section:** <link or heading>
**API contract section:** <endpoint(s) consumed, if any — or "n/a">

## Why

<1–3 sentences. What user-visible outcome this enables, or why it's a needed step toward one. Stay above implementation: name behavior, not classes.>

## Acceptance criteria

The contract Construction will satisfy. Observable and testable. Embed the relevant test in this list — do NOT split tests into trailing stories.

- [ ] <Given/When/Then or behavioral statement, framed in user or API terms>
- [ ] <Edge case — empty, error, timeout, etc.>
- [ ] Test for this slice exists and passes (Construction chooses the test shape).
- [ ] The lane's standard build/test commands pass with no regressions. (See [CLAUDE.md](CLAUDE.md) or `<lane>/CLAUDE.md` for the exact commands.)

## Blocked by

- <wikilinks to other stories in the same lane, or "nothing — independently grabbable">

## Hints (non-binding)

> [!tip]
> Hints help Construction orient. They are NOT a contract. Construction reads
> the lane's CLAUDE.md + opens existing similar files and may diverge from
> any hint here without re-opening Inception.

- **Likely files affected:** `<rough paths>` — confirm against the actual code before editing.
- **Existing pattern to mirror:** `<file>` — Construction inspects this and matches its conventions.
- **Watch out for:** `<known constraint, regression risk, or coordination point>`

## Out of scope for this story

- <Things adjacent that look related but belong to a different story or feature.>
