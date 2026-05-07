package com.mindnote.server

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll

data class Account(
    val id: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Long,
)

object AuthAccounts : Table("auth_accounts") {
    val id = varchar("id", 64)
    val username = varchar("username", 30).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object AuthTokens : Table("auth_tokens") {
    val token = varchar("token", 128)
    val accountId = varchar("account_id", 64)
        .references(AuthAccounts.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(token)

    init { index(false, accountId) }
}

/**
 * Resolve the [Account] owning [token], or null if the token is unknown / revoked.
 * Caller must already be inside a `transaction { ... }` block.
 */
fun resolveAccountFromToken(token: String): Account? =
    AuthTokens.innerJoin(AuthAccounts, { accountId }, { id })
        .selectAll()
        .where { AuthTokens.token eq token }
        .firstOrNull()
        ?.let {
            Account(
                id = it[AuthAccounts.id],
                username = it[AuthAccounts.username],
                passwordHash = it[AuthAccounts.passwordHash],
                createdAt = it[AuthAccounts.createdAt],
            )
        }
