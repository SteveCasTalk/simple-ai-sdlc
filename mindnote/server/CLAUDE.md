# MindNote server — agent guide

> [!note]
> Ktor backend for the MindNote app. Separate Gradle build from `app/`. This file
> is the canonical stack/conventions reference for any AI agent working in
> `server/`. Read before proposing dependencies or new patterns.

## Stack

Authoritative source: [`build.gradle.kts`](build.gradle.kts).

| Concern | Library | Version |
|---|---|---|
| Language | Kotlin (JVM 17) | 2.0.20 |
| **HTTP server** | **Ktor (Netty engine)** | **3.0.3** |
| HTTP client (outbound) | Ktor client (OkHttp engine) | 3.0.3 |
| Serialization | kotlinx.serialization JSON | 1.7.3 |
| **DB access** | **Exposed** (`exposed-core`, `exposed-jdbc`, `exposed-java-time`) | **0.57.0** |
| DB driver | PostgreSQL JDBC | 42.7.4 |
| Connection pool | HikariCP | 6.2.1 |
| Logging | Logback Classic | 1.5.12 |
| Build / packaging | `io.ktor.plugin` (fat-jar) | 3.0.3 |
| Deployment | Railway ([railway.json](railway.json), [Dockerfile](Dockerfile)) | — |

If a feature seems to need something not in the table above, surface the proposal and get user confirmation before adding it — don't quietly introduce a new dep.

## Architecture

Flat package `com.mindnote.server.*` — small server, no enforced layering. Each file has one job:

```
src/main/kotlin/com/mindnote/server/
├── Application.kt        ← main(), embeddedServer(Netty), plugin install
├── Routes.kt             ← REST routes (notes, favorites, etc.)
├── ChatRoutes.kt         ← chat-specific routes (SSE)
├── ChatProvider.kt       ← provider abstraction for chat backend
├── ChatTables.kt         ← Exposed table objects for chat
├── Database.kt           ← Hikari + Exposed Database.connect setup
├── Models.kt             ← domain types
└── Dto.kt                ← @Serializable wire types
```

Plugins installed in `Application.kt`: `ContentNegotiation` (kotlinx.serialization), `CallLogging`, `StatusPages`, `DefaultHeaders`, `CORS`, `SSE`.

## Conventions

### Routes
Defined as Ktor route extension blocks in `Routes.kt` / `ChatRoutes.kt`:

```kotlin
fun Route.notes() {
    get("/notes") { ... call.respond(...) }
    post("/notes") {
        val body = call.receive<NoteCreateDto>()
        ...
    }
}
```

Wired into `Application.module()` via `routing { notes(); chatRoutes() }`. Add a new route group → add a sibling `XxxRoutes.kt` with one extension function and call it from `module()`.

### DTOs
All wire types live in [Dto.kt](src/main/kotlin/com/mindnote/server/Dto.kt) as `@Serializable` data classes. DTOs are distinct from domain `Models`. Snake-case JSON fields use `@SerialName`.

### Persistence (Exposed)
Tables are `object`s extending `Table`. Queries run inside `transaction { ... }` (or the suspending `newSuspendedTransaction { ... }` from `exposed-jdbc-async` — verify the existing pattern in `Routes.kt` before choosing). DB setup: `Database.kt` configures HikariCP from env vars (`DATABASE_URL` etc.) and calls `Database.connect`.

### Errors
Use `StatusPages` to map exceptions → HTTP responses globally. Don't try/catch in route handlers unless converting to a domain-specific error envelope. Error envelope shape lives in `Dto.kt`.

### Config
Env-var driven (`PORT`, `DATABASE_URL`, etc.) — read directly via `System.getenv`. No `application.conf` HOCON file in this project.

## Deployment
Railway with the Ktor fat-jar (`./gradlew :server:buildFatJar` → `mindnote-server.jar`). [Dockerfile](Dockerfile) + [railway.json](railway.json) define the runtime. See [README.md](README.md) for the deploy loop.

## When in doubt
Open the closest existing file in the same folder before inventing a new pattern.
