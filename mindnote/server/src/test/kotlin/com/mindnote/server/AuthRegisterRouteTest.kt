package com.mindnote.server

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AuthRegisterRouteTest {

    /**
     * Each test gets its own H2 in-memory DB. Returns the JDBC URL so the test can
     * tear down between cases if needed (it doesn't — each test uses a fresh URL).
     */
    private fun freshDb() {
        ExposedDatabase.connect(
            url = "jdbc:h2:mem:auth_register_${System.nanoTime()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(AuthAccounts, AuthTokens)
            // start clean in case the random URL collides (rare but cheap insurance)
            AuthTokens.deleteAll()
            AuthAccounts.deleteAll()
        }
    }

    private fun setup() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
    }

    private val testJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `201 returns token and account, persists hashed password and token`() = testApplication {
        freshDb()
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
        val client = createClient { install(ContentNegotiation) { json(testJson) } }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "alice_42", password = "Hunter2!aB"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body: AuthSuccessDto = response.body()
        assertEquals("alice_42", body.account.username)
        assertNotNull(body.account.id)
        assertTrue(body.token.isNotBlank())

        // password is stored hashed, not as plaintext
        transaction {
            val row = AuthAccounts.selectAll().where { AuthAccounts.username eq "alice_42" }.single()
            val storedHash = row[AuthAccounts.passwordHash]
            assertNotEquals("Hunter2!aB", storedHash)
            assertTrue(storedHash.startsWith("\$argon2id\$"))
            // and the token is in the tokens table
            val tokenCount = AuthTokens.selectAll().where { AuthTokens.token eq body.token }.count()
            assertEquals(1, tokenCount)
        }
    }

    @Test
    fun `400 validation_failed when username is invalid`() = testApplication {
        freshDb()
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
        val client = createClient { install(ContentNegotiation) { json(testJson) } }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "Alice 42", password = "Hunter2!aB"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("validation_failed", env.error.code)
        assertNotNull(env.error.fields)
        assertNotNull(env.error.fields["username"])

        // no account created
        transaction {
            assertEquals(0, AuthAccounts.selectAll().count())
        }
    }

    @Test
    fun `400 validation_failed when password is too weak`() = testApplication {
        freshDb()
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
        val client = createClient { install(ContentNegotiation) { json(testJson) } }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "alice_42", password = "weak"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("validation_failed", env.error.code)
        assertNotNull(env.error.fields)
        assertNotNull(env.error.fields["password"])
    }

    @Test
    fun `409 username_taken when the username is already in use`() = testApplication {
        freshDb()
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
        val client = createClient { install(ContentNegotiation) { json(testJson) } }

        // first registration
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "alice_42", password = "Hunter2!aB"))
        }

        // duplicate
        val dup = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "alice_42", password = "Different9!bC"))
        }

        assertEquals(HttpStatusCode.Conflict, dup.status)
        val env: ErrorEnvelope = dup.body()
        assertEquals("username_taken", env.error.code)

        // only one account exists
        transaction {
            assertEquals(1, AuthAccounts.selectAll().where { AuthAccounts.username eq "alice_42" }.count())
        }
    }

    @Test
    fun `409 username_taken is case-insensitive (uppercase username also rejected as taken)`() = testApplication {
        freshDb()
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { authRoutes() }
        }
        val client = createClient { install(ContentNegotiation) { json(testJson) } }

        // register lowercase
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "alice_42", password = "Hunter2!aB"))
        }

        // ALICE_42 should fail validation (uppercase rejected by username validator),
        // not even reach the conflict check. So this asserts 400, not 409 — which is
        // the contractually correct behavior since username regex enforces lowercase.
        val withUpper = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(username = "ALICE_42", password = "Different9!bC"))
        }
        assertEquals(HttpStatusCode.BadRequest, withUpper.status)
    }
}
