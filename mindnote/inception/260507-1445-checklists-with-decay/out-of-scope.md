---
type: out-of-scope
feature: checklists-with-decay
created: 2026-05-07
tags:
  - inception/out-of-scope
  - feature/checklists-with-decay
---

# Out of scope

> [!warning]
> Things this feature is explicitly **not** doing in v1. The cheapest argument to prevent is one we wrote down. Each of these is a defensible candidate for a future Inception — promote them as separate features when the mob is ready.

- **Push notifications** — about expiring or archived items. Excluded because it contradicts the "force confrontation when user engages" spirit, and would balloon scope (FCM, server scheduling, OS permissions). See D8.
- **Nesting / sub-items** — flat lists only in v1. Indented sub-tasks are a v2 candidate; tree CRUD doubles the API surface.
- **Per-item "pin / never decay" opt-out** — workaround in v1 is to toggle done off→on→off (or just edit the text) to refresh `last_touched_at`. A real opt-out flag is a small v2 add.
- **Bulk operations** — rescue-all, archive-all, delete-all-completed, export-as-markdown. Power-user features; revisit after retention data.
- **Tags or due dates on individual items** — items have only `text`, `done`, decay metadata. The note-level `tags` already covers categorization; due dates would require a calendar/reminders system that doesn't exist.
- **Calendar / reminders / system integrations** — driver explicitly said no in Step 1.5.
- **iOS client** — the project has no iOS lane (`settings.gradle.kts` only includes `:app`). When iOS is added, this feature's API contract is already iOS-friendly — the iOS team picks up the same endpoints.
- **Markdown export of items** — items roundtrip through the API as structured Block objects, not as markdown checkboxes. If the user wants to copy-paste a checklist out of mindnote, v1 will give them the prose-only body. A markdown serializer is a v2 add.
- **Search across archived items** — global search defaults to excluding archived. If a user wants to find an archived item, they open the parent note and expand the archive section. Search-includes-archived is a v2 toggle.
- **Cross-device live update of decay** — opacity is computed client-side from `last_touched_at`. If device A touches an item, device B doesn't update its rendered opacity until the next refresh from server. Acceptable for v1 (single user, mobile patterns of use).
- **Rescue confirmation dialog** — tapping `Rescue` immediately rescues. No "Are you sure?" — rescue is non-destructive (it un-archives an item; nothing is lost if it was a misclick — the user can simply ignore the item, and it will re-archive in 60 days).
- **Custom decay durations** — 30/60 days are fixed in v1. Per-user or per-note configuration is a v2 add.
- **Archive of WHOLE notes** (vs. items) — archived state in v1 is per-item only. Archiving entire notes is a different feature.
