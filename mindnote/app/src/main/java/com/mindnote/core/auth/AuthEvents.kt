package com.mindnote.core.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide signal for "the user has been signed out by the server" — emitted when any
 * authenticated request returns 401 (token revoked / unknown).
 *
 * Held as a Koin singleton so the HTTP layer (which emits) and the nav layer (which
 * navigates) can communicate without coupling. The nav layer must subscribe early —
 * this is a hot signal with no replay; a late subscriber misses the event by design.
 */
class AuthEvents {
    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    suspend fun emitSignedOut() {
        _signedOut.emit(Unit)
    }
}
