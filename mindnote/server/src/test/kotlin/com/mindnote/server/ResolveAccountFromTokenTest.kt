package com.mindnote.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class ResolveAccountFromTokenTest {

    private fun withDb(block: () -> Unit) {
        ExposedDatabase.connect(
            url = "jdbc:h2:mem:test_${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(AuthAccounts, AuthTokens)
            block()
        }
    }

    @Test
    fun `valid token resolves to its account`() = withDb {
        AuthAccounts.insert {
            it[id] = "acct-1"
            it[username] = "alice_42"
            it[passwordHash] = "hash-1"
            it[createdAt] = 1_000L
        }
        AuthTokens.insert {
            it[token] = "tok-good"
            it[accountId] = "acct-1"
            it[createdAt] = 2_000L
        }

        val resolved = resolveAccountFromToken("tok-good")

        assertEquals(
            Account(id = "acct-1", username = "alice_42", passwordHash = "hash-1", createdAt = 1_000L),
            resolved,
        )
    }

    @Test
    fun `bogus token resolves to null`() = withDb {
        AuthAccounts.insert {
            it[id] = "acct-1"
            it[username] = "alice_42"
            it[passwordHash] = "hash-1"
            it[createdAt] = 1L
        }
        AuthTokens.insert {
            it[token] = "real-token"
            it[accountId] = "acct-1"
            it[createdAt] = 2L
        }

        assertNull(resolveAccountFromToken("not-a-real-token"))
    }

    @Test
    fun `each account's token resolves to that account when multiple accounts exist`() = withDb {
        AuthAccounts.insert {
            it[id] = "acct-1"; it[username] = "alice"; it[passwordHash] = "h1"; it[createdAt] = 1L
        }
        AuthAccounts.insert {
            it[id] = "acct-2"; it[username] = "bob"; it[passwordHash] = "h2"; it[createdAt] = 2L
        }
        AuthTokens.insert { it[token] = "tok-alice"; it[accountId] = "acct-1"; it[createdAt] = 10L }
        AuthTokens.insert { it[token] = "tok-bob"; it[accountId] = "acct-2"; it[createdAt] = 20L }

        assertEquals("alice", resolveAccountFromToken("tok-alice")?.username)
        assertEquals("bob", resolveAccountFromToken("tok-bob")?.username)
    }
}
