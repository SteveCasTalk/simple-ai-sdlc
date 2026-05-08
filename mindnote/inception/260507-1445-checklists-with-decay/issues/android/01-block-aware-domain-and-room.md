---
type: issue
feature: checklists-with-decay
lane: android
status: ready
wave: 0
estimate: 90m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/checklists-with-decay
  - status/ready
  - wave/0
---

# [AN] Domain `Note` + Room schema model `body` as `List<Block>`

**Lane:** Android
**PRD section:** [[PRD#Story 5 — Items roundtrip correctly through the API]], [[PRD#Constraints]] (migration policy).
**API contract section:** [[api-contract#Block shape]], [[api-contract#GET /notes/{id}]].

## Why

This is plumbing that everything else builds on. The Android domain `Note` model and Room schema currently store `body` as a `String`. To render or interact with checklist items, the entire data path — DTO ↔ domain ↔ Room ↔ ViewModel — must carry `body` as `List<Block>`. No user-observable UI change in this slice; the deliverable is a *programmer-observable* contract: "the repo returns blocks now."

## Acceptance criteria

- [ ] Domain model: `Note.body` is `List<Block>` where `Block` is a sealed type / hierarchy with two cases: `ProseBlock(text: String)` and `ItemBlock(id: String, text: String, done: Boolean, lastTouchedAt: Instant, archivedAt: Instant?)`.
- [ ] Room schema migration: existing notes' string `body` is migrated to a single `ProseBlock` on first read. Either via a Room `Migration` that rewrites the column, or via an adapter layer that lazily converts on load — Construction picks. Migration test verifies an existing-shape note round-trips correctly post-migration.
- [ ] DTO layer (the wire `Note` Ktor receives) deserializes `body` per the API contract's `Block` JSON shape (sealed `prose` / `item` discriminator).
- [ ] `NotesRepository` and any consumer (`HomeViewModel`, `NotesScreen`'s ViewModel, the note-detail VM) compiles with `body: List<Block>`. *Render integration is out of scope here* — the rendering story (02) does the UI work. This story is purely structural.
- [ ] App still compiles, launches, and shows existing notes' prose content somewhere (acceptable to display only the joined text of all `ProseBlock`s as a stopgap until story 02 lands real rendering).
- [ ] Lane verify passes: `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

## Blocked by

- Nothing — independently grabbable. Foundation for Android lane.
- *(Note: Android can develop against a stub server / mock until the BE foundation [[../backend/01-block-shaped-notes-foundation]] is deployed; integration testing requires it.)*

## Hints (non-binding)

- **Likely files affected:** `app/src/main/java/com/mindnote/domain/model/Note.kt` (or wherever the domain `Note` lives), the Room `NoteEntity`, the `NotesDao`, any DTO mapping in `data/remote/`, and the existing repos. Confirm against [`mindnote/CLAUDE.md`](mindnote/CLAUDE.md).
- **Block sealed hierarchy:** Kotlin sealed class with two data classes is conventional. Make sure kotlinx.serialization is configured with the correct discriminator for the wire shape.
- **Room storage:** the simplest path is a `TypeConverter` that serializes `List<Block>` to JSON (so Room sees a String column under the hood). The auth feature's existing patterns for non-trivial converters are a guide.
- **Watch out for:** the existing `Note.preview` field is currently the first 100 chars of `body`. With body now being structured, define `preview` as the first 100 chars of the *joined `ProseBlock` text* (or the first non-empty prose block). Mob can override.

## Out of scope for this story

- Rendering blocks in a Compose UI (story 02).
- Item interactions (stories 03–08).
- Decay opacity computation (story 02).
