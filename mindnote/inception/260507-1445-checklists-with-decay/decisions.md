---
type: decisions
feature: checklists-with-decay
created: 2026-05-07
tags:
  - inception/decisions
  - feature/checklists-with-decay
---

# Decisions

> [!info]
> ADR-lite log. Driver-made; mob ratifies in review. Each entry exists to prevent re-litigation in week 3.

---

### D1 — Bundle checklist CRUD pre-req with decay UX (Y1) — 2026-05-07

- **Context:** mindnote has no checklist concept today (`Note.body` is a plain string, no checkbox, no item entity). The decay feature requires checklist items to exist in the first place.
- **Options considered:**
  - **Y1 (chosen):** ship checklists + decay together as one feature.
  - **Y2:** split into two Inceptions — checklists v1, then decay later.
- **Decision:** Y1.
- **Why:** Plain checklists in mindnote add nothing over markdown-in-body without the decay mechanism. The UX problem this feature solves is "LARP-ing tasks", which only manifests once items decay. Shipping checklists alone would be a half-feature with no clear user-facing differentiator.
- **Consequences:** This Inception is larger (foundation + UX in one). Compensate by sequencing stories so the foundation is wave-0 / wave-1 and decay UX layers on top — the project is still parallelizable across BE and Android, just internally sequenced.

### D2 — Note body becomes `List<Block>` with prose + item types (Y-prose-mixed) — 2026-05-07

- **Context:** Two ways to represent items: markdown-in-body string with sidecar metadata (M), or first-class blocks (B). Plus two layouts: prose-then-items (P) or interleaved (I).
- **Options considered:**
  - **M+P:** body stays a string with markdown checkboxes; items are a separate flat list rendered after prose.
  - **M+I:** body stays a string; items are anchored back into prose via line-hash sidecar.
  - **B+P:** body becomes `{prose: string, items: ChecklistItem[]}` — items rendered after prose.
  - **B+I (chosen):** body becomes `List<Block>` where each block is `prose` or `item`, freely interleaved.
- **Decision:** B+I (Y-prose-mixed, Y storage = first-class entity).
- **Why:** Sidecar approaches are fragile (text-edit drift breaks the anchor). Prose-then-items rules out the actual user motion of "thought, action, thought, action" that the feature is designed for. First-class blocks with interleaving cost a one-time migration but give clean per-item touch semantics.
- **Consequences:** Existing notes' `body` (string) migrates to `[{type: "prose", text: <old body>}]`. Existing client code that reads `Note.body: String` breaks and must be updated everywhere. API shape change is breaking but mindnote has no third-party API consumers, so the cost is contained to our two clients (web doesn't exist yet; iOS doesn't either).

### D3 — Server is the timekeeping authority (option 2a) — 2026-05-07

- **Context:** `last_touched_at` could live client-side, server-side, or be computed server-side as a fade level the server emits.
- **Options considered:**
  - **2a (chosen):** server stores `last_touched_at`; client renders fade locally from that timestamp.
  - **2b:** server emits a pre-computed `fade_level: 0.0..1.0` field. Client just renders.
  - **2c:** full server-side everything (including push of fade-level changes).
- **Decision:** 2a.
- **Why:** Cheapest to implement, cheap to scale, and gives the client snappy rendering without per-scroll round trips. `last_touched_at` is also the natural authoritative timestamp the auto-archive cron needs. Multi-device conflict resolution is trivially "last write wins on `last_touched_at`."
- **Consequences:** Client must trust device clock for the rendering pass. Acceptable: a wrong device clock just makes the *visual* fade off by hours/days; the *data* (touch events, archive transitions) is always server-authoritative.

### D4 — Decay curve: linear opacity 100% → 20% over 30 days, constant 20% to 60d — 2026-05-07

- **Context:** "Nearly invisible at 30 days" needs an actual function so client renderers and server-side test fixtures agree.
- **Options considered:** linear, ease-out, stepped (e.g. drop opacity once at 7d, again at 21d), exponential.
- **Decision:** Linear from day 0 (100%) to day 30 (20%). Constant 20% from day 30 to day 60. At day 60, item auto-archives (no longer in live list).
- **Why:** Linear is simplest and predictable. The 20% floor stops items from being literally invisible (which would be confusing — users would think they were deleted). The 30→60 plateau gives users time to notice and act before auto-archive.
- **Consequences:** The exact formula `opacity = max(0.2, 1.0 - (days_since_last_touch / 30) * 0.8)` is part of the contract — both client and server-side test fixtures use it.

### D5 — Touch semantics: toggle done / edit text / reorder — opening or scrolling does NOT touch — 2026-05-07

- **Context:** "Untouched" needs a precise definition or the feature degenerates (e.g. if opening the note touches every item, decay never triggers).
- **Decision:** A `Touch` is exactly: toggling done state, editing the item's text (any change to the text field on save), or reordering the item via drag handle. **Not a touch:** opening the note, scrolling past, viewing the archived expander, editing a *different* item in the same note, editing prose blocks in the same note.
- **Why:** Decay is meant to reflect engagement with the *specific item*, not just the note as a whole. Counting note-level activity as a per-item touch would defeat the purpose ("just opened the note, everything is fresh again").
- **Consequences:** Server-side, every per-item endpoint mutates `last_touched_at`. The notes-level `PATCH /notes/{id}` only resets `last_touched_at` for items whose `text` or `done` value actually changed in the request — not for items that just rode along in the body array.

### D6 — Done items don't decay visually, but DO auto-archive at 60d — 2026-05-07

- **Context:** Should completed items also fade and archive?
- **Decision:** Done items stay at 100% opacity (they're successfully completed — no LARP penalty). However, the daily cron archives any item 60+ days untouched regardless of done state — this declutters the note over time.
- **Why:** Visual decay is a guilt mechanism for un-acted-on items. Completed items deserve to be readable. But long-completed items still take up screen real estate, so archiving them is just hygiene.
- **Consequences:** The 60d archive boundary is uniform; only the visual treatment between 0 and 60d differs by done state. Acceptable for v1.

### D7 — Archived items live in a per-note expander, not a global archive — 2026-05-07

- **Context:** Where should archived items go?
- **Options considered:**
  - **(a)** A global "Archived items" screen (cross-note).
  - **(b — chosen)** In their parent note, hidden behind a `▶ Show N archived` expander at the bottom of the live list.
  - **(c)** Soft-delete (gone from view; rescuable from a hidden settings screen).
- **Decision:** (b).
- **Why:** Locality. Archived items belong to their original context; a global archive screen creates a "mortuary" surface separate from the work. The expander keeps archived items one tap away — close enough to rescue, far enough to declutter.
- **Consequences:** Cross-note views (e.g. search) need to decide whether to include archived items. **Default: exclude.** Search to include archived is a v2 affordance.

### D8 — No push notifications in v1 — 2026-05-07

- **Context:** Driver flagged "we might need to add scope" for push.
- **Decision:** Out of scope. The feature works in-app: users discover decay/archive when they open their notes.
- **Why:** Push contradicts the spirit of decay — which is "force confrontation when the user *chooses* to engage" — and adds a multi-week side-quest (FCM, server scheduling, OS permissions). The driver agreed in Step 1.
- **Consequences:** No nag mechanism. Users who never open mindnote miss the auto-archive entirely until they do — *which is fine*: that's the whole point.

### D9 — Existing notes are migrated, not wiped — 2026-05-07

- **Context:** The auth feature wiped existing notes. This feature changes `Note.body` from `String` to `List<Block>`. Migrate or wipe?
- **Decision:** Migrate. Each existing note's string body becomes `[{type: "prose", text: <old body>}]`. No data loss.
- **Why:** Auth wiped because the user-account model changed fundamentally and there was no concept of ownership pre-feature. The block migration is purely additive on the same data — no semantic regress, just a structural lift.
- **Consequences:** A migration test fixture with at least one pre-feature note is part of BE-1's acceptance.
