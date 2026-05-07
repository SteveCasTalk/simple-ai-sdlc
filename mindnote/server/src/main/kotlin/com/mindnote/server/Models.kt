package com.mindnote.server

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.ReferenceOption

const val LOCAL_USER_ID = "local"

object Users : Table("users") {
    val id = varchar("id", 64)
    val username = varchar("username", 128).default("")
    override val primaryKey = PrimaryKey(id)

    fun ensureLocalUser() {
        insertIgnore {
            it[id] = LOCAL_USER_ID
            it[username] = ""
        }
    }
}

object Notes : Table("notes") {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 64).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val title = text("title")
    val preview = text("preview")
    val body = text("body")
    val date = date("date")
    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
    }
}

object Topics : Table("topics") {
    val name = varchar("name", 64)
    override val primaryKey = PrimaryKey(name)
}

object NoteTopics : Table("note_topics") {
    val noteId = varchar("note_id", 64).references(Notes.id, onDelete = ReferenceOption.CASCADE)
    val topicName = varchar("topic_name", 64).references(Topics.name, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(noteId, topicName)

    init {
        index(false, topicName)
    }
}

object Favorites : Table("favorites") {
    val userId = varchar("user_id", 64).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val noteId = varchar("note_id", 64).references(Notes.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(userId, noteId)

    init {
        index(false, noteId)
    }
}
