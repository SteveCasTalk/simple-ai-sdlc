package com.mindnote.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.mindnote.core.storage.UserPrefs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import kotlinx.coroutines.flow.first
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BearerAttachTest {

    private lateinit var tempDir: File
    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("bearer-attach-test").toFile()
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

    private fun clientWith(prefs: UserPrefs): Pair<HttpClient, MutableList<String?>> {
        val capturedAuth = mutableListOf<String?>()
        val engine = MockEngine { request ->
            capturedAuth += request.headers[HttpHeaders.Authorization]
            respond("ok", HttpStatusCode.OK, headersOf())
        }
        val client = HttpClient(engine) {
            install(BearerAuth) {
                tokenSource = { prefs.authTokenFlow.first() }
            }
        }
        return client to capturedAuth
    }

    @Test
    fun `header present when a token is stored`() = withPrefs { prefs ->
        prefs.setAuthToken("opaque-tok-abc")
        val (client, captured) = clientWith(prefs)

        client.get("https://example.test/notes")

        assertEquals("Bearer opaque-tok-abc", captured.single())
    }

    @Test
    fun `header absent when no token is stored`() = withPrefs { prefs ->
        // do not setAuthToken
        val (client, captured) = clientWith(prefs)

        client.get("https://example.test/auth/login")

        assertNull(captured.single())
    }

    @Test
    fun `subsequent requests pick up token changes without rebuilding the client`() = withPrefs { prefs ->
        val (client, captured) = clientWith(prefs)

        // 1) no token
        client.get("https://example.test/notes")
        // 2) set a token
        prefs.setAuthToken("first-tok")
        client.get("https://example.test/notes")
        // 3) replace it
        prefs.setAuthToken("second-tok")
        client.get("https://example.test/notes")
        // 4) clear it
        prefs.clearAuthToken()
        client.get("https://example.test/auth/login")

        assertEquals(
            listOf(null, "Bearer first-tok", "Bearer second-tok", null),
            captured,
        )
    }
}
