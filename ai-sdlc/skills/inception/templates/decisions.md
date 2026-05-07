---
type: decisions
feature: <feature-slug>
created: <YYYY-MM-DD>
tags:
  - inception/decisions
  - feature/<feature-slug>
---

# Decisions

> [!info]
> ADR-lite log. One entry per decision the mob made (or that the driver made and the mob ratified). The point is to prevent re-litigating the same argument in week 3.

## Format

### D<n> — <one-line decision> — <YYYY-MM-DD>

- **Context:** <what made this decision necessary>
- **Options considered:** <the alternatives>
- **Decision:** <what we chose>
- **Why:** <the reasoning, in one paragraph>
- **Consequences:** <what this commits us to / what we give up>

---

### D1 — <example: Use cursor pagination for the feed endpoint> — <YYYY-MM-DD>

- **Context:** Feed could grow to 10k+ items per user; offset pagination would be slow and unstable across writes.
- **Options considered:** Offset pagination, cursor pagination, no pagination (load all).
- **Decision:** Cursor pagination keyed on `created_at + id`.
- **Why:** Stable across inserts, performant at scale, mobile clients can resume scrolling without re-fetching.
- **Consequences:** Clients cannot jump to "page N". Acceptable — the UX is infinite scroll.
