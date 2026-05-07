---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 0
estimate: 60m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/0
---

# [Android] Add `imagePath` to `Note` (domain + entity + Room migration + DTO mapping)

**Lane:** Android
**PRD section:** Goals — original photo retained alongside the saved note.
**API contract section:** N/A (server `NoteDto` is unchanged — see [[../../decisions|D4]]).

## Why

Establishes a clean data-model home for the original photo so the Scan flow's Save step ([[05-ocr-call-review-and-save]]) can persist `imagePath` directly instead of stuffing it into `body`. Existing screens (Home, Notes, Note detail, Capture) keep working unchanged since the field is nullable.

## Implementation steps

1. **Domain model** — `app/src/main/java/com/mindnote/domain/model/Models.kt`:
   ```kotlin
   data class Note(
       val id: String,
       val title: String,
       val preview: String,
       val body: String,
       val tags: List<String>,
       val date: LocalDate,
       val imagePath: String? = null,
   )
   ```
   The default `null` keeps every existing call-site compiling.
2. **Room entity** — `app/src/main/java/com/mindnote/data/db/entities/NoteEntity.kt`: add `val imagePath: String? = null`.
3. **Room database** — `app/src/main/java/com/mindnote/data/db/MindNoteDatabase.kt`:
   - Bump `@Database(version = 2, ...)`.
   - Add migration constant inside the `companion object`:
     ```kotlin
     val MIGRATION_1_2 = object : Migration(1, 2) {
         override fun migrate(db: SupportSQLiteDatabase) {
             db.execSQL("ALTER TABLE notes ADD COLUMN imagePath TEXT")
         }
     }
     ```
   - Wire `.addMigrations(MIGRATION_1_2)` wherever the Room builder lives (search `Room.databaseBuilder` — likely in `core/di/AppModule.kt`).
4. **Mapping `NoteEntity → Note`** — `app/src/main/java/com/mindnote/data/repository/NoteMapping.kt`: pass `imagePath = note.imagePath` through `toDomain`.
5. **Mapping server `NoteDto → NoteEntity`** — `app/src/main/java/com/mindnote/data/remote/DtoMapping.kt`:
   - Server has no `imagePath`. The mapping must **preserve** the existing local value when a server refresh writes back. Two implementation options:
     - (Preferred) Modify the upsert path in `RoomNotesRepository` (or wherever `NoteDto` is persisted) so it does **not** overwrite `imagePath` on conflict. Use a partial update / `@Query("UPDATE notes SET title=:t, preview=:p, body=:b, date=:d WHERE id=:id")` instead of `INSERT … OR REPLACE` for the server-refresh case.
     - Or: load the existing entity, copy `imagePath` from it, then re-insert.
   - Pick whichever is cleaner with how `RoomNotesRepository` already does refresh; document the choice in the PR description.
6. **`NoteCreateDto` payload to server** — leave unchanged. `Note.imagePath` is local-only and is **not** sent up; `Note.toCreateDto()` ignores it.
7. Update unit tests:
   - `DtoMappingTest` — add a case asserting that mapping a fresh `NoteDto → NoteEntity` produces `imagePath = null`.
   - New test `RoomNotesRepositoryImagePathTest` (or extend an existing one): insert a `NoteEntity` with `imagePath = "/data/.../scan.jpg"`, simulate a server refresh that returns the same `id` with no image info, assert `imagePath` is still `"/data/.../scan.jpg"` after refresh.

## Files to touch

- `app/src/main/java/com/mindnote/domain/model/Models.kt` — modify.
- `app/src/main/java/com/mindnote/data/db/entities/NoteEntity.kt` — modify.
- `app/src/main/java/com/mindnote/data/db/MindNoteDatabase.kt` — modify (version + migration constant).
- `app/src/main/java/com/mindnote/core/di/AppModule.kt` — modify (wire migration into builder).
- `app/src/main/java/com/mindnote/data/repository/NoteMapping.kt` — modify.
- `app/src/main/java/com/mindnote/data/repository/RoomNotesRepository.kt` — modify (preserve `imagePath` on server refresh; verify exact strategy after reading the file).
- `app/src/main/java/com/mindnote/data/remote/DtoMapping.kt` — review; likely no change needed if step 5 happens at the repository layer.
- `app/src/test/java/com/mindnote/data/remote/DtoMappingTest.kt` — modify.
- New test for the imagePath-preservation case.

## Acceptance criteria

- [ ] `Note` domain model has nullable `imagePath`. Existing call-sites still compile (no required arg).
- [ ] `NoteEntity` has the column; Room migration `1 → 2` adds it without data loss on existing dev installs.
- [ ] A server refresh of a note that has a local `imagePath` does **not** overwrite the field with `null`.
- [ ] `NoteCreateDto` sent to the server does not include `imagePath` (server contract unchanged).
- [ ] Unit test: existing `DtoMappingTest` passes with the added case for `imagePath = null` on fresh DTO mapping.
- [ ] Unit test: imagePath-preservation case passes.
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` succeeds.
- [ ] Manual smoke: launch app on a device that already has the v1 DB; notes load without error and existing note bodies are intact.

## Blocked by

- Nothing — independently grabbable.

## Notes

- This issue does **not** populate `imagePath`. That's the job of [[05-ocr-call-review-and-save]]. This issue just makes the field exist and the schema safe.
- If `RoomNotesRepository` currently uses `OnConflictStrategy.REPLACE` on its insert, that's the source of the overwrite risk — switch to a partial-update strategy or a read-modify-write before merging.
- Don't fall back to `fallbackToDestructiveMigration()` — even in a demo it makes Room migration patterns easy to copy-paste wrong elsewhere.
