# MindNote — agent guide

> [!note]
> This file declares the Android app's stack, conventions, and folder layout
> for any AI agent operating on the repo (inception, dev claim, refactors).
> Read this BEFORE proposing any new dependency or implementation pattern.

## Stack (Android — `app/`)

Authoritative source: [`gradle/libs.versions.toml`](gradle/libs.versions.toml). Do not introduce libraries not already declared there without explicit user approval.

| Concern | Library | Version |
|---|---|---|
| Language | Kotlin | 2.0.20 |
| Build | AGP, KSP | 8.5.2 / 2.0.20-1.0.25 |
| UI | Jetpack Compose (BOM) + Material3 | 2024.09.03 |
| Navigation | androidx.navigation.compose | 2.8.1 |
| **HTTP client** | **Ktor (OkHttp engine)** | **3.0.3** |
| Serialization | kotlinx.serialization JSON | 1.7.3 |
| **DI** | **Koin** (`koin-android`, `koin-androidx-compose`) | **4.0.0** |
| **Persistence** | **Room** (with KSP) | **2.6.1** |
| Preferences | androidx.datastore-preferences | 1.1.1 |
| Paging | androidx.paging (+ compose) | 3.3.2 |
| Async | kotlinx.coroutines | 1.9.0 |
| Test | JUnit 4, kotlinx.coroutines.test | 4.13.2 |

If a feature seems to need something not in the table above, surface the proposal and get user confirmation before adding it — don't quietly introduce a new dep.

## Architecture

```
app/src/main/java/com/mindnote/
├── core/{di, ext, mvi, navigation, storage}   ← cross-cutting
├── data/
│   ├── db/{dao, entities}                      ← Room
│   ├── remote/                                 ← Ktor API classes
│   └── repository/                             ← Room*Repository implements domain
├── domain/{model, repository}                  ← interfaces + entities
├── design/                                     ← design system / theme
└── features/{capture, chat, home, notedetail, notes, onboarding}
```

Pattern: **MVI** (see `core/mvi`). Each feature has a `*ViewModel` exposing state + intents.

## Conventions

### HTTP / API classes
All API classes follow this Ktor pattern (see [NotesApi.kt](app/src/main/java/com/mindnote/data/remote/NotesApi.kt) and [ChatApi.kt](app/src/main/java/com/mindnote/data/remote/ChatApi.kt)):

```kotlin
class XxxApi(private val client: HttpClient) {
    suspend fun foo(...): FooDto =
        client.get("path").body()

    suspend fun bar(body: BarDto): BarResponse =
        client.post("path") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
}
```

DTOs are `@Serializable` data classes co-located in `data/remote/`. The shared `HttpClient` is provided by Koin in [`core/di/AppModule.kt`](app/src/main/java/com/mindnote/core/di/AppModule.kt) — never construct one inline.

### DI (Koin)
Single `appModule` in `core/di/AppModule.kt`. Adding a new service/repo/VM means appending to that module:

```kotlin
single { OcrApi(get()) }                     // service
single<OcrRepository> { KtorOcrRepository(get()) }  // repository binding
viewModel { ScanViewModel(get(), get()) }    // VM
```

### Persistence (Room)
Database: `MindNoteDatabase` in [`data/db/`](app/src/main/java/com/mindnote/data/db). Entities under `data/db/entities/`, DAOs under `data/db/dao/`. Add a column → bump `MindNoteDatabase` version, add a `Migration`, register it in the Room builder in `AppModule.kt`.

### UI / Navigation
Single-activity Compose. Routes registered in `core/navigation/`. Each feature owns its `*Screen.kt` composable + `*ViewModel.kt`.

## Server (`server/`)
Separate Gradle build (Ktor server, Railway-deployed). See [server/README.md](server/README.md).

## When in doubt
Open the closest existing file in the same folder before inventing a new pattern.
