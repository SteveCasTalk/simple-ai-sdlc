package com.mindnote.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.mindnote.core.auth.AuthEvents
import com.mindnote.core.storage.UserPrefs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRevokedTest {

    private lateinit var tempDir: File
    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("auth-revoked-test").toFile()
        prefsFile = File(tempDir, "test.preferences_pb")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun <T> withPrefs(block: suspend (UserPrefs) -> T): T = runBlocking {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        val store: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { prefsFile })
        try {
            block(UserPrefs(store))
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun client(events: AuthEvents, prefs: UserPrefs, status: HttpStatusCode): HttpClient {
        val engine = MockEngine { respond("body", status, headersOf()) }
        return HttpClient(engine) {
            install(AuthRevoked) {
                this.userPrefs = prefs
                this.authEvents = events
            }
        }
    }

    @Test
    fun `401 on a non-auth call clears the token and emits signedOut`() = withPrefs { prefs ->
        prefs.setAuthToken("about-to-be-revoked")
        val events = AuthEvents()
        val received = mutableListOf<Unit>()

        runTest {
            val collector = launch { events.signedOut.collect { received += it } }
            val c = client(events, prefs, HttpStatusCode.Unauthorized)

            runCatching { c.get("https://example.test/notes") }

            // give the plugin's onResponse a chance to run
            kotlinx.coroutines.delay(50)
            collector.cancel()
        }

        // token cleared + event was observed
        assertNull(prefs.authTokenFlow.first())
        assertEquals(1, received.size)
    }

    @Test
    fun `200 leaves the token alone and emits nothing`() = withPrefs { prefs ->
        prefs.setAuthToken("still-valid")
        val events = AuthEvents()
        val received = mutableListOf<Unit>()

        runTest {
            val collector = launch { events.signedOut.collect { received += it } }
            val c = client(events, prefs, HttpStatusCode.OK)

            c.get("https://example.test/notes")

            kotlinx.coroutines.delay(50)
            collector.cancel()
        }

        assertEquals("still-valid", prefs.authTokenFlow.first())
        assertEquals(0, received.size)
    }

    @Test
    fun `401 on a path under auth slash is ignored (login expects its own 401)`() = withPrefs { prefs ->
        prefs.setAuthToken("opaque")
        val events = AuthEvents()
        val received = mutableListOf<Unit>()

        runTest {
            val collector = launch { events.signedOut.collect { received += it } }
            val c = client(events, prefs, HttpStatusCode.Unauthorized)

            runCatching { c.get("https://example.test/auth/login") }

            kotlinx.coroutines.delay(50)
            collector.cancel()
        }

        // token NOT cleared, no signedOut emitted
        assertEquals("opaque", prefs.authTokenFlow.first())
        assertEquals(0, received.size)
    }
}
