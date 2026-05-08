---
type: issue
feature: checklists-with-decay
lane: backend
status: ready
wave: 0
estimate: 90m
blocked-by: []
tags:
  - inception/issue
  - lane/backend
  - feature/checklists-with-decay
  - status/ready
  - wave/0
---

# [BE] Notes endpoints carry `body: List<Block>` (prose + item) instead of `body: String`

**Lane:** Backend
**PRD section:** [[PRD#Story 5 — Items roundtrip correctly through the API]], [[PRD#Constraints]] (migration policy).
**API contract section:** [[api-contract#Block shape]], [[api-contract#GET /notes/{id}]], [[api-contract#GET /notes]], [[api-contract#POST /notes]], [[api-contract#PATCH /notes/{id}]].

## Why

The decay UX has nowhere to live until a `Note.body` can carry first-class items. This story is the foundation: extend the wire shape so every existing `/notes` endpoint serializes `body` as `List<Block>` (with `prose` blocks only at this stage — items endpoints come in subsequent stories). Without this, no client can render anything new.

This also locks in the *migration policy*: existing notes do not lose data. Each pre-feature note's plain-string `body` becomes a single `[{type: "prose", text: <old body>}]` block on the first read.

## Acceptance criteria

- [ ] `GET /notes/{id}` returns `body: List<Block>` and a sibling `archived_count: int` (always `0` at this stage, since no items exist yet).
- [ ] `GET /notes` returns the same shape per element in the list.
- [ ] `POST /notes` accepts a request with `body: List<Block>` and persists prose blocks correctly. Item blocks may be in the request payload but are out of scope for this story — Construction may either ignore them and respond 200 (preferred for forward-compat) or 400 with a clear message. **TBD-internal:** pick whichever is cleaner; document the choice in the test.
- [ ] `PATCH /notes/{id}` accepts a `body: List<Block>` and replaces the stored body. Stricter rule per [[open-questions#Q3]] default: if `body` is omitted from the request, the body is unchanged; if `body` is present, it is replaced wholesale.
- [ ] **Migration:** any pre-feature note (whose `body` is currently a string in storage) is read back as `[{type: "prose", text: <old body>}]`. Verified by a fixture of at least one pre-feature note created against the old schema.
- [ ] An integration test fetches a pre-feature note, asserts the prose block round-trip; creates a new note with mixed prose blocks, fetches it, asserts the order and text are preserved.
- [ ] Lane verify passes: `./gradlew :server:test :server:build`.

## Blocked by

- Nothing — independently grabbable. This is the foundation everything else depends on.

## Hints (non-binding)

> [!tip]
> Hints orient Construction. Not a contract. Construction reads `server/CLAUDE.md` and the existing `Routes.kt` / `Models.kt` / `Dto.kt` patterns and may diverge.

- **Likely files affected:** `server/src/main/kotlin/com/mindnote/server/Models.kt`, `Dto.kt`, `Routes.kt`, plus a Flyway-style migration or Exposed `SchemaUtils.create` change. Confirm against `server/CLAUDE.md`.
- **Storage choice for body:** the `body` column may stay a single column (e.g. JSONB) or become a `note_blocks` table — Construction picks based on Exposed ergonomics + test cost. Inception is agnostic.
- **Existing pattern to mirror:** the auth feature's `[[mindnote/inception/260504-1122-username-password-auth/issues/backend/01-account-and-token-schema|01-account-and-token-schema]]` for schema-migration test style.
- **Watch out for:** existing Android client deserializes `body` as `String`. This story breaks that read. Coordinate with [[../android/01-block-aware-domain-and-room]] — Android lands its half against the new shape (using a stub or dev server) and the integration goes hot when both ship.

## Out of scope for this story

- Items CRUD (POST/PATCH/DELETE on `/notes/{id}/items`) — see stories 02–04.
- The `archived_at` field on items — appears in the schema as a column with `NULL` default, but no logic populates it yet. Cron logic lands in story 06.
- The `?include_archived=true` query param — story 05.
