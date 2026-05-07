package com.mindnote.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {

    private fun api(handler: MockRequestHandler): AuthApi {
        val engine = MockEngine(handler)
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        return AuthApi(client)
    }

    private fun jsonResponse(status: HttpStatusCode, body: String): MockRequestHandler = {
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    // ----- register -----

    @Test
    fun `register 201 returns Success with token and account`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.Created,
            """{"token":"opaque-abc","account":{"id":"acct-1","username":"alice_42"}}"""
        ))

        val result = api.register("alice_42", "Hunter2!aB")

        result as AuthResult.Success
        assertEquals("opaque-abc", result.value.token)
        assertEquals("acct-1", result.value.account.id)
        assertEquals("alice_42", result.value.account.username)
    }

    @Test
    fun `register 409 returns Failure with username_taken code`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.Conflict,
            """{"error":{"code":"username_taken","message":"username is already in use"}}"""
        ))

        val result = api.register("alice_42", "Hunter2!aB")

        result as AuthResult.Failure
        assertEquals(AuthErrorCodes.USERNAME_TAKEN, result.code)
        assertEquals("username is already in use", result.message)
    }

    @Test
    fun `register 400 returns Failure with validation_failed code`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.BadRequest,
            """{"error":{"code":"validation_failed","message":"bad password","fields":{"password":"must contain a digit"}}}"""
        ))

        val result = api.register("alice_42", "weak")

        result as AuthResult.Failure
        assertEquals(AuthErrorCodes.VALIDATION_FAILED, result.code)
    }

    @Test
    fun `register on network failure returns NetworkError`() = runTest {
        val api = api { throw IOException("no connectivity") }

        val result = api.register("alice_42", "Hunter2!aB")

        assertSame(AuthResult.NetworkError, result)
    }

    @Test
    fun `register sends username and password as JSON body`() = runTest {
        var capturedBody: String? = null
        val api = api { request ->
            capturedBody = (request.body as TextContent).text
            respond(
                """{"token":"t","account":{"id":"a","username":"alice_42"}}""",
                HttpStatusCode.Created,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        api.register("alice_42", "Hunter2!aB")

        assertTrue("body should contain username, was: $capturedBody", capturedBody!!.contains("alice_42"))
        assertTrue("body should contain password, was: $capturedBody", capturedBody!!.contains("Hunter2!aB"))
    }

    // ----- login -----

    @Test
    fun `login 200 returns Success with token and account`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.OK,
            """{"token":"login-tok","account":{"id":"acct-1","username":"alice_42"}}"""
        ))

        val result = api.login("alice_42", "Hunter2!aB")

        result as AuthResult.Success
        assertEquals("login-tok", result.value.token)
        assertEquals("alice_42", result.value.account.username)
    }

    @Test
    fun `login 401 returns Failure with invalid_credentials code`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.Unauthorized,
            """{"error":{"code":"invalid_credentials","message":"username or password is wrong"}}"""
        ))

        val result = api.login("alice_42", "wrong")

        result as AuthResult.Failure
        assertEquals(AuthErrorCodes.INVALID_CREDENTIALS, result.code)
    }

    @Test
    fun `login on network failure returns NetworkError`() = runTest {
        val api = api { throw IOException("dns lookup failed") }

        val result = api.login("alice_42", "Hunter2!aB")

        assertSame(AuthResult.NetworkError, result)
    }

    // ----- logout -----

    @Test
    fun `logout 204 returns Success`() = runTest {
        val api = api {
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        val result = api.logout("opaque-tok")

        result as AuthResult.Success
        assertEquals(Unit, result.value)
    }

    @Test
    fun `logout 401 returns Failure with invalid_token code`() = runTest {
        val api = api(jsonResponse(
            HttpStatusCode.Unauthorized,
            """{"error":{"code":"invalid_token","message":"unknown or revoked token"}}"""
        ))

        val result = api.logout("stale-tok")

        result as AuthResult.Failure
        assertEquals(AuthErrorCodes.INVALID_TOKEN, result.code)
    }

    @Test
    fun `logout on network failure returns NetworkError`() = runTest {
        val api = api { throw IOException("offline") }

        val result = api.logout("opaque-tok")

        assertSame(AuthResult.NetworkError, result)
    }

    @Test
    fun `logout sends the token in the Authorization Bearer header`() = runTest {
        var capturedAuth: String? = null
        val api = api { request ->
            capturedAuth = request.headers[HttpHeaders.Authorization]
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        api.logout("opaque-tok-abc")

        assertEquals("Bearer opaque-tok-abc", capturedAuth)
    }
}
