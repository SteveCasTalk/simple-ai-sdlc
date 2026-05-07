package com.mindnote.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class AuthApi(
    private val client: HttpClient,
    private val json: Json = DefaultJson,
) {

    suspend fun register(username: String, password: String): AuthResult<AuthSuccessDto> = tryCall {
        client.post("auth/register") { setBody(RegisterRequest(username, password)) }
            .body<AuthSuccessDto>()
    }

    suspend fun login(username: String, password: String): AuthResult<AuthSuccessDto> = tryCall {
        client.post("auth/login") { setBody(LoginRequest(username, password)) }
            .body<AuthSuccessDto>()
    }

    suspend fun logout(token: String): AuthResult<Unit> = tryCall {
        client.post("auth/logout") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        Unit
    }

    private suspend inline fun <T> tryCall(block: () -> T): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (e: ClientRequestException) {
        e.toFailure()
    } catch (e: IOException) {
        AuthResult.NetworkError
    }

    private suspend fun ClientRequestException.toFailure(): AuthResult<Nothing> {
        val raw = response.bodyAsText()
        val envelope = try {
            json.decodeFromString(AuthErrorEnvelopeDto.serializer(), raw)
        } catch (_: SerializationException) {
            return AuthResult.Failure(code = "unknown", message = raw)
        }
        return AuthResult.Failure(code = envelope.error.code, message = envelope.error.message)
    }

    private companion object {
        val DefaultJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
