package com.mindnote.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `AuthSuccessDto deserializes token and account`() {
        val parsed = json.decodeFromString<AuthSuccessDto>(
            """{"token":"opaque-abc","account":{"id":"acct-1","username":"alice_42"}}"""
        )
        assertEquals("opaque-abc", parsed.token)
        assertEquals("acct-1", parsed.account.id)
        assertEquals("alice_42", parsed.account.username)
    }

    @Test
    fun `RegisterRequest serializes to username and password fields`() {
        val req = RegisterRequest(username = "alice_42", password = "Hunter2!aB")
        val encoded = json.encodeToString(RegisterRequest.serializer(), req)
        val reparsed = json.decodeFromString<RegisterRequest>(encoded)
        assertEquals("alice_42", reparsed.username)
        assertEquals("Hunter2!aB", reparsed.password)
    }

    @Test
    fun `LoginRequest serializes to username and password fields`() {
        val req = LoginRequest(username = "alice_42", password = "Hunter2!aB")
        val encoded = json.encodeToString(LoginRequest.serializer(), req)
        val reparsed = json.decodeFromString<LoginRequest>(encoded)
        assertEquals("alice_42", reparsed.username)
        assertEquals("Hunter2!aB", reparsed.password)
    }

    @Test
    fun `AuthErrorEnvelopeDto deserializes code and message`() {
        val parsed = json.decodeFromString<AuthErrorEnvelopeDto>(
            """{"error":{"code":"invalid_credentials","message":"username or password is wrong"}}"""
        )
        assertEquals("invalid_credentials", parsed.error.code)
        assertEquals("username or password is wrong", parsed.error.message)
    }

    @Test
    fun `AuthErrorEnvelopeDto tolerates the optional fields object on validation_failed`() {
        val parsed = json.decodeFromString<AuthErrorEnvelopeDto>(
            """{"error":{"code":"validation_failed","message":"...","fields":{"password":"must contain a digit"}}}"""
        )
        assertEquals("validation_failed", parsed.error.code)
        assertEquals("...", parsed.error.message)
    }

    @Test
    fun `AuthResult Success carries the typed value`() {
        val result: AuthResult<AuthSuccessDto> = AuthResult.Success(
            AuthSuccessDto(token = "t", account = AccountDto(id = "a1", username = "a"))
        )
        assertEquals("t", (result as AuthResult.Success).value.token)
    }

    @Test
    fun `AuthResult Failure carries the server code and message`() {
        val result: AuthResult<Nothing> = AuthResult.Failure(
            code = AuthErrorCodes.USERNAME_TAKEN,
            message = "username is already in use",
        )
        assertEquals("username_taken", (result as AuthResult.Failure).code)
        assertEquals("username is already in use", result.message)
    }

    @Test
    fun `AuthResult NetworkError is a singleton object`() {
        val a: AuthResult<Nothing> = AuthResult.NetworkError
        val b: AuthResult<Nothing> = AuthResult.NetworkError
        // both refs point at the same object
        assertEquals(a, b)
        assertNull((a as? AuthResult.Failure))
    }

    @Test
    fun `AuthErrorCodes constants match the api-contract codes`() {
        assertEquals("validation_failed", AuthErrorCodes.VALIDATION_FAILED)
        assertEquals("username_taken", AuthErrorCodes.USERNAME_TAKEN)
        assertEquals("invalid_credentials", AuthErrorCodes.INVALID_CREDENTIALS)
        assertEquals("unauthenticated", AuthErrorCodes.UNAUTHENTICATED)
        assertEquals("invalid_token", AuthErrorCodes.INVALID_TOKEN)
    }
}
