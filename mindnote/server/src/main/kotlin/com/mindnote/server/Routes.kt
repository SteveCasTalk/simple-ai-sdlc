package com.mindnote.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal const val DEVICE_ID_HEADER = "X-Device-Id"

/** Resolve the caller's user id from `X-Device-Id` and make sure the row exists. */
internal fun ApplicationCall.userIdFromHeader(): String =
    request.headers[DEVICE_ID_HEADER]?.takeIf { it.isNotBlank() } ?: LOCAL_USER_ID

internal fun ensureUser(userId: String) {
    Users.insertIgnore {
        it[Users.id] = userId
        it[username] = ""
    }
}

fun Route.notesRoutes() {
    route("/notes") {
        get {
            val userId = call.userIdFromHeader()
            val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 30).coerceIn(1, 100)
            val notes = newSuspendedTransaction {
                ensureUser(userId)
                loadNotesForUser(userId, offset, limit)
            }
            call.respond(notes)
        }
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val userId = call.userIdFromHeader()
            val note = newSuspendedTransaction { loadNote(id, userId) }
            if (note == null) call.respond(HttpStatusCode.NotFound) else call.respond(note)
        }
        post {
            val body = call.receive<NoteCreateDto>()
            val userId = call.userIdFromHeader()
            val saved = newSuspendedTransaction {
                ensureUser(userId)
                insertNote(body, userId)
            }
            call.respond(HttpStatusCode.Created, saved)
        }
        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val userId = call.userIdFromHeader()
            val deleted = newSuspendedTransaction {
                Notes.deleteWhere { (Notes.id eq id) and (Notes.userId eq userId) }
            }
            if (deleted > 0) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}

fun Route.favoritesRoutes() {
    route("/favorites") {
        get {
            val userId = call.userIdFromHeader()
            val favs = newSuspendedTransaction {
                ensureUser(userId)
                val ids = Favorites
                    .selectAll()
                    .where { Favorites.userId eq userId }
                    .map { it[Favorites.noteId] }
                ids.mapNotNull { loadNote(it, userId) }
            }
            call.respond(favs)
        }
        post("{noteId}") {
            val noteIdParam = call.parameters["noteId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val uid = call.userIdFromHeader()
            newSuspendedTransaction {
                ensureUser(uid)
                Favorites.insertIgnore {
                    it[userId] = uid
                    it[noteId] = noteIdParam
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
        delete("{noteId}") {
            val noteIdParam = call.parameters["noteId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val uid = call.userIdFromHeader()
            newSuspendedTransaction {
                Favorites.deleteWhere {
                    (userId eq uid) and (noteId eq noteIdParam)
                }
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun loadNotesForUser(userId: String, offset: Int = 0, limit: Int = Int.MAX_VALUE): List<NoteDto> {
    val rows = Notes
        .selectAll()
        .where { Notes.userId eq userId }
        .orderBy(Notes.date to SortOrder.DESC, Notes.id to SortOrder.DESC)
        .limit(limit, offset.toLong())
        .toList()

    if (rows.isEmpty()) return emptyList()

    val ids = rows.map { it[Notes.id] }
    val tagsByNote: Map<String, List<String>> = NoteTopics
        .selectAll()
        .where { NoteTopics.noteId inList ids }
        .groupBy({ it[NoteTopics.noteId] }, { it[NoteTopics.topicName] })

    return rows.map { row ->
        NoteDto(
            id = row[Notes.id],
            title = row[Notes.title],
            preview = row[Notes.preview],
            body = row[Notes.body],
            tags = tagsByNote[row[Notes.id]].orEmpty(),
            date = row[Notes.date].toString(),
        )
    }
}

internal fun loadNote(id: String, userId: String): NoteDto? {
    val row = Notes
        .selectAll()
        .where { (Notes.id eq id) and (Notes.userId eq userId) }
        .firstOrNull() ?: return null
    val tags = NoteTopics
        .selectAll()
        .where { NoteTopics.noteId eq id }
        .map { it[NoteTopics.topicName] }
    return NoteDto(
        id = row[Notes.id],
        title = row[Notes.title],
        preview = row[Notes.preview],
        body = row[Notes.body],
        tags = tags,
        date = row[Notes.date].toString(),
    )
}

private fun insertNote(input: NoteCreateDto, userId: String): NoteDto {
    val id = input.id ?: "n-${System.currentTimeMillis()}"
    val date = input.date?.let(LocalDate::parse) ?: LocalDate.now()
    Notes.insert {
        it[Notes.id] = id
        it[Notes.userId] = userId
        it[title] = input.title.ifBlank { "Untitled note" }
        it[preview] = input.preview.ifBlank { input.body.lineSequence().firstOrNull().orEmpty().take(120) }
        it[body] = input.body
        it[Notes.date] = date
    }
    input.tags.forEach { tag ->
        Topics.insertIgnore { it[name] = tag }
        NoteTopics.insertIgnore {
            it[noteId] = id
            it[topicName] = tag
        }
    }
    return loadNote(id, userId)!!
}
