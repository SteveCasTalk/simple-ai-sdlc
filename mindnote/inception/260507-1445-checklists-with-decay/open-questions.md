---
type: open-questions
feature: checklists-with-decay
created: 2026-05-07
tags:
  - inception/open-questions
  - feature/checklists-with-decay
---

# Open questions

> [!question]
> Driver's parking lot. Anything the driver could not resolve alone goes here for the mob to answer in review.
>
> **Inception ends when this file is empty** (or only contains items the mob explicitly deferred to a later phase).

## Open

### Q1 — Item delete UX surface: long-press menu vs. swipe?

- **Why it matters:** Affects the Android delete story (AN-7) and influences the editor's gesture vocabulary. Long-press is consistent with current mindnote UI; swipe is more discoverable.
- **[DRIVER GUESS]:** Long-press item → contextual menu with `Delete` (matches existing list patterns in mindnote).
- **[ASKED OF]:** Android / Product

### Q2 — `GET /notes` list responses: full body, or truncated prefix?

- **Why it matters:** The notes list screen currently fetches a paged list of notes. With body now a `List<Block>`, payloads grow proportionally to item count. If a user has 30 notes with 50 items each, that's a meaningful payload. Construction may want to truncate (e.g. first 5 blocks per note + a `body_truncated: true` flag).
- **[DRIVER GUESS]:** Ship full body in v1 (mindnote is single-user — payload size won't bite for a long time). Add truncation only if measurements show the cost.
- **[ASKED OF]:** BE / Product

### Q3 — `PATCH /notes/{id}` with `body` omitting items: delete those items, or preserve them?

- **Why it matters:** If a client updates only the title via `PATCH /notes/{id}` and forgets to include the existing items in the body field, the server (per current contract) deletes the items. Footgun. Alternative: a `?preserve_items=true` query flag, or a stricter rule (`body` omitted entirely = no body change; `body` present = full replace).
- **[DRIVER GUESS]:** Stricter rule. If `body` is **absent** from the request, do not touch the body. If `body` is **present**, full replace (current rule). This pushes clients toward the per-item endpoints for item changes, which is the desired pattern anyway.
- **[ASKED OF]:** BE

### Q4 — `PATCH /notes/{id}/items/{item_id}` on an archived item: 409 or silent rescue?

- **Why it matters:** A user might tap-toggle an archived item via "Show archived" inline. Current contract says return 409 — client must call `/rescue` first. Alternative: silently rescue + apply the patch (simpler client code, but hides a state transition).
- **[DRIVER GUESS]:** 409. Explicit rescue is healthier — the user should know their action is reviving an archived thing.
- **[ASKED OF]:** Product / BE

### Q5 — `DELETE /notes/{id}/items/{item_id}` idempotency: 204 or 404 on missing?

- **Why it matters:** Affects retry/replay safety. Idempotent (always 204) is friendlier to flaky-network mobile clients. Strict (404 on missing) catches client bugs sooner.
- **[DRIVER GUESS]:** 204 idempotent. Mobile network reality favors retry-safety.
- **[ASKED OF]:** BE

## Resolved

_(none yet — first run.)_
