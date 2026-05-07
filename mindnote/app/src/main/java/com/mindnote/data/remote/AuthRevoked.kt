package com.mindnote.data.remote

import com.mindnote.core.auth.AuthEvents
import com.mindnote.core.storage.UserPrefs
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode

/**
 * Ktor client plugin that watches every response and, on a 401, clears the local token
 * and emits an [AuthEvents.signedOut] signal so the nav layer can route back to Login.
 *
 * Skips paths under `/auth/...` because login intentionally returns 401 on bad
 * credentials — that's not a token-revoked signal, just a wrong-password signal.
 *
 * The token-clear has to happen here (not in the nav layer) so that the *next* request
 * doesn't accidentally re-attach the now-invalid token via [BearerAuth]. The nav signal
 * is decoupled because the HTTP client has no Compose / nav scope.
 */
class AuthRevokedConfig {
    var userPrefs: UserPrefs? = null
    var authEvents: AuthEvents? = null
}

val AuthRevoked = createClientPlugin("AuthRevoked", ::AuthRevokedConfig) {
    val userPrefs = requireNotNull(pluginConfig.userPrefs) { "AuthRevoked.userPrefs not configured" }
    val events = requireNotNull(pluginConfig.authEvents) { "AuthRevoked.authEvents not configured" }

    onResponse { response ->
        if (response.status != HttpStatusCode.Unauthorized) return@onResponse
        // Login / register / logout return their own 401s for their own reasons (wrong creds,
        // already-revoked token). Don't conflate those with "your session is gone".
        if (response.call.request.url.encodedPath.startsWith("/auth/")) return@onResponse

        userPrefs.clearAuthToken()
        events.emitSignedOut()
    }
}
