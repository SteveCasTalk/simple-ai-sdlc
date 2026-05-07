package com.mindnote.data.remote

/**
 * Result of an auth API call.
 *
 * - [Success] — 2xx; carries the typed response body.
 * - [Failure] — 4xx; carries the server's error envelope `code` + `message` so the caller
 *   can match on [AuthErrorCodes]. Not an exception, so the code never gets lost.
 * - [NetworkError] — connectivity / server unreachable / timeout. Distinguishable from a 4xx
 *   so the UI can show "you're offline" vs. "wrong password".
 */
sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val code: String, val message: String) : AuthResult<Nothing>
    data object NetworkError : AuthResult<Nothing>
}

/** Server-defined error codes from the auth api-contract. */
object AuthErrorCodes {
    const val VALIDATION_FAILED = "validation_failed"
    const val USERNAME_TAKEN = "username_taken"
    const val INVALID_CREDENTIALS = "invalid_credentials"
    const val UNAUTHENTICATED = "unauthenticated"
    const val INVALID_TOKEN = "invalid_token"
}
