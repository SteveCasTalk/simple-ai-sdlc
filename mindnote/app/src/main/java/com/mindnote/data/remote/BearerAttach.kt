package com.mindnote.data.remote

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Ktor client plugin that attaches `Authorization: Bearer <token>` to every outbound request
 * when [BearerAuthConfig.tokenSource] returns a non-null value.
 *
 * Read at request time (not at install time), so token changes (login, logout) take effect
 * on subsequent requests without rebuilding the client. Routes that must stay unauthenticated
 * (login, register) work because when the user is signed out the source returns null and the
 * header is simply absent.
 */
class BearerAuthConfig {
    /** Returns the current token, or null if the user is signed out. */
    var tokenSource: suspend () -> String? = { null }
}

val BearerAuth = createClientPlugin("BearerAuth", ::BearerAuthConfig) {
    val source = pluginConfig.tokenSource
    onRequest { request, _ ->
        val token = source()
        if (token != null) {
            request.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
