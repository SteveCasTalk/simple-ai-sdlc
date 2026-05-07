package com.mindnote.server

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

fun Route.chatRoutes(provider: ChatProvider) {

    get("/conversations/latest") {
        val uid = call.userIdFromHeader()
        val latestId = newSuspendedTransaction {
            Conversations
                .selectAll()
                .where { Conversations.userId eq uid }
                .orderBy(Conversations.createdAt to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { it[Conversations.id] }
        }
        call.respond(LatestConversationDto(id = latestId))
    }

    route("/conversations/{id}") {

        get("/messages") {
            val convoId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val uid = call.userIdFromHeader()
            val before = call.request.queryParameters["before"]?.toLongOrNull()
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 30).coerceIn(1, 100)
            val msgs = newSuspendedTransaction { loadMessagesPage(convoId, uid, before, limit) }
            call.respond(msgs)
        }

        route("/stream", HttpMethod.Post) { sse {
            val convoId = call.parameters["id"] ?: run {
                close(); return@sse
            }
            val uid = call.userIdFromHeader()
            val body = call.receive<SendMessageDto>()
            val userText = body.text.trim()
            if (userText.isEmpty()) {
                close(); return@sse
            }

            val turns = newSuspendedTransaction {
                ensureUser(uid)
                ensureConversation(convoId, uid)
                insertMessage(convoId, role = "user", content = userText)
                Conversations
                    .selectAll()
                    .where { Conversations.id eq convoId }
                    .firstOrNull()
                    ?.let { row ->
                        if (row[Conversations.title].isBlank()) {
                            Conversations.update({ Conversations.id eq convoId }) {
                                it[title] = userText.take(60)
                            }
                        }
                    }
                loadMessagesFor(convoId, uid).map { ChatTurn(it.role, it.content) }
            }

            val system = buildSystemPrompt(uid)
            val assistantBuffer = StringBuilder()
            try {
                provider.stream(system, turns).collect { token ->
                    assistantBuffer.append(token)
                    send(ServerSentEvent(event = "token", data = token))
                }
                val finalText = assistantBuffer.toString()
                if (finalText.isNotEmpty()) {
                    newSuspendedTransaction {
                        insertMessage(convoId, role = "assistant", content = finalText)
                    }
                }
                send(ServerSentEvent(event = "done", data = ""))
            } catch (t: Throwable) {
                send(ServerSentEvent(event = "error", data = t.message.orEmpty()))
            }
        } }
    }
}

private fun ensureConversation(id: String, userId: String) {
    Conversations.insertIgnore {
        it[Conversations.id] = id
        it[Conversations.userId] = userId
        it[title] = ""
        it[createdAt] = System.currentTimeMillis()
    }
}

private fun insertMessage(conversationId: String, role: String, content: String) {
    ChatMessages.insert {
        it[id] = "m-${System.currentTimeMillis()}-${(0..999).random()}"
        it[ChatMessages.conversationId] = conversationId
        it[ChatMessages.role] = role
        it[ChatMessages.content] = content
        it[createdAt] = System.currentTimeMillis()
    }
}

private fun loadMessagesFor(conversationId: String, userId: String): List<ChatMessageDto> {
    val owned = Conversations
        .selectAll()
        .where { (Conversations.id eq conversationId) and (Conversations.userId eq userId) }
        .any()
    if (!owned) return emptyList()
    return ChatMessages
        .selectAll()
        .where { ChatMessages.conversationId eq conversationId }
        .orderBy(ChatMessages.createdAt to SortOrder.ASC, ChatMessages.id to SortOrder.ASC)
        .map {
            ChatMessageDto(
                id = it[ChatMessages.id],
                role = it[ChatMessages.role],
                content = it[ChatMessages.content],
                createdAt = it[ChatMessages.createdAt],
            )
        }
}

/**
 * Cursor-paged page of messages, newest-first. `before` is an exclusive `createdAt` cursor:
 * pass the oldest `createdAt` the client already has to load the previous page.
 * Returns at most [limit] rows; fewer means end-of-history reached.
 */
private fun loadMessagesPage(
    conversationId: String,
    userId: String,
    before: Long?,
    limit: Int,
): List<ChatMessageDto> {
    val owned = Conversations
        .selectAll()
        .where { (Conversations.id eq conversationId) and (Conversations.userId eq userId) }
        .any()
    if (!owned) return emptyList()
    return ChatMessages
        .selectAll()
        .where {
            val base = ChatMessages.conversationId eq conversationId
            if (before != null) base and (ChatMessages.createdAt less before) else base
        }
        .orderBy(ChatMessages.createdAt to SortOrder.DESC, ChatMessages.id to SortOrder.DESC)
        .limit(limit)
        .map {
            ChatMessageDto(
                id = it[ChatMessages.id],
                role = it[ChatMessages.role],
                content = it[ChatMessages.content],
                createdAt = it[ChatMessages.createdAt],
            )
        }
}

private fun buildSystemPrompt(userId: String): String = transaction {
    val notes = Notes
        .selectAll()
        .where { Notes.userId eq userId }
        .orderBy(Notes.date, SortOrder.DESC)
        .toList()

    if (notes.isEmpty()) {
        return@transaction DEFAULT_SYSTEM
    }

    buildString {
        appendLine(DEFAULT_SYSTEM)
        appendLine()
        appendLine("The user's notes (most recent first):")
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        notes.forEach { row ->
            val title = row[Notes.title]
            val date = row[Notes.date].format(fmt)
            val preview = row[Notes.preview].takeIf { it.isNotBlank() } ?: row[Notes.body].take(200)
            appendLine("- [$date] $title — $preview")
        }
    }
}

private const val DEFAULT_SYSTEM = """You are MindNote, a memory-first assistant for the user's personal notes.
Answer based on the notes below when the user's question relates to them — quote or reference note titles when useful.
If a question is unrelated, answer normally. Be concise and conversational."""

