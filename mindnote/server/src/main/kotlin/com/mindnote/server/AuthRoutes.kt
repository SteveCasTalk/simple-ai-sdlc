package com.mindnote.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.security.SecureRandom
import java.util.UUID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/** Auth routes: /auth/register, /auth/login, /auth/logout. */
fun Route.authRoutes() {
    route("/auth") {
        post("/register") { call.handleRegister() }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.handleRegister() {
    val body = receive<RegisterRequestDto>()

    val usernameError = validateUsername(body.username)
    val passwordError = validatePassword(body.password)
    if (usernameError != null || passwordError != null) {
        val fields = buildMap {
            usernameError?.let { put("username", it) }
            passwordError?.let { put("password", it) }
        }
        respond(
            HttpStatusCode.BadRequest,
            ErrorEnvelope(ErrorBody(
                code = "validation_failed",
                message = "username or password is invalid",
                fields = fields,
            )),
        )
        return
    }

    val passwordHash = Passwords.hash(body.password)
    val accountId = "acct-${UUID.randomUUID()}"
    val token = newToken()
    val now = System.currentTimeMillis()

    val created = newSuspendedTransaction {
        // duplicate check first (case-insensitive — usernames are stored lowercase)
        val taken = AuthAccounts.selectAll()
            .where { AuthAccounts.username eq body.username }
            .any()
        if (taken) return@newSuspendedTransaction null

        try {
            AuthAccounts.insert {
                it[id] = accountId
                it[username] = body.username
                it[AuthAccounts.passwordHash] = passwordHash
                it[createdAt] = now
            }
        } catch (e: ExposedSQLException) {
            // race: another thread inserted the same username between our check and insert
            return@newSuspendedTransaction null
        }
        AuthTokens.insert {
            it[AuthTokens.token] = token
            it[AuthTokens.accountId] = accountId
            it[AuthTokens.createdAt] = now
        }
        AuthSuccessDto(token = token, account = AccountDto(id = accountId, username = body.username))
    }

    if (created == null) {
        respond(
            HttpStatusCode.Conflict,
            ErrorEnvelope(ErrorBody(code = "username_taken", message = "username is already in use")),
        )
        return
    }
    respond(HttpStatusCode.Created, created)
}

private val tokenRng = SecureRandom()

internal fun newToken(): String {
    val bytes = ByteArray(32)
    tokenRng.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}
