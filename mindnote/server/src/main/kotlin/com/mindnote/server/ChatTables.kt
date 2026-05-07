package com.mindnote.server

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Conversations : Table("conversations") {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 64).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val title = text("title").default("")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)

    init { index(false, userId) }
}

object ChatMessages : Table("chat_messages") {
    val id = varchar("id", 64)
    val conversationId = varchar("conversation_id", 64)
        .references(Conversations.id, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 16) // "user" | "assistant"
    val content = text("content")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)

    init { index(false, conversationId) }
}
