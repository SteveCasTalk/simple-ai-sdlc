package com.mindnote.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URI
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    fun init() {
        val ds = HikariDataSource(hikariConfig())
        ExposedDatabase.connect(ds)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Notes, Topics, NoteTopics, Favorites,
                Conversations, ChatMessages,
                AuthAccounts, AuthTokens,
            )
            // FK target for all notes/favorites — single-user demo
            Users.ensureLocalUser()
        }
    }

    private fun hikariConfig(): HikariConfig {
        val raw = System.getenv("DATABASE_URL")
        val (jdbc, user, pass) = when {
            raw.isNullOrBlank() -> Triple(
                "jdbc:postgresql://localhost:5432/mindnote",
                System.getenv("PGUSER") ?: "postgres",
                System.getenv("PGPASSWORD") ?: "postgres",
            )
            raw.startsWith("jdbc:") -> Triple(raw, System.getenv("PGUSER"), System.getenv("PGPASSWORD"))
            else -> parsePostgresUrl(raw)
        }
        return HikariConfig().apply {
            jdbcUrl = jdbc
            driverClassName = "org.postgresql.Driver"
            if (!user.isNullOrBlank()) username = user
            if (!pass.isNullOrBlank()) password = pass
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
    }

    // postgres://user:pass@host:port/db  →  jdbc:postgresql://host:port/db + user + pass
    private fun parsePostgresUrl(url: String): Triple<String, String?, String?> {
        val u = URI(url)
        val userInfo = u.userInfo?.split(":", limit = 2)
        val user = userInfo?.getOrNull(0)
        val pass = userInfo?.getOrNull(1)
        val host = u.host
        val port = if (u.port == -1) 5432 else u.port
        val db = u.path.removePrefix("/")
        val query = if (u.rawQuery.isNullOrBlank()) "" else "?${u.rawQuery}"
        return Triple("jdbc:postgresql://$host:$port/$db$query", user, pass)
    }
}
