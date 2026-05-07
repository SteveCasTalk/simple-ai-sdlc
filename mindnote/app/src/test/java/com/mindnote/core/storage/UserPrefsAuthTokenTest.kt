package com.mindnote.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserPrefsAuthTokenTest {

    private lateinit var tempDir: File
    private lateinit var prefsFile: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("user-prefs-test").toFile()
        prefsFile = File(tempDir, "test.preferences_pb")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    /**
     * Run [block] with a UserPrefs backed by a real DataStore over [prefsFile], then
     * fully cancel the DataStore's scope so the file lock is released before the next
     * instance is created (DataStore enforces one active instance per file per process).
     */
    private fun <T> withFreshUserPrefs(block: suspend (UserPrefs) -> T): T = runBlocking {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        val store: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { prefsFile })
        val result = block(UserPrefs(store))
        job.cancelAndJoin()
        result
    }

    @Test
    fun `unset token reads as null`() {
        withFreshUserPrefs { prefs ->
            assertNull(prefs.authTokenFlow.first())
        }
    }

    @Test
    fun `save then read returns the same token`() {
        withFreshUserPrefs { prefs ->
            prefs.setAuthToken("opaque-token-abc")
            assertEquals("opaque-token-abc", prefs.authTokenFlow.first())
        }
    }

    @Test
    fun `clear after save resets the token to null`() {
        withFreshUserPrefs { prefs ->
            prefs.setAuthToken("opaque-token-abc")
            prefs.clearAuthToken()
            assertNull(prefs.authTokenFlow.first())
        }
    }

    @Test
    fun `token survives a fresh UserPrefs over the same backing file`() {
        withFreshUserPrefs { writer -> writer.setAuthToken("survives-restart") }

        val readBack = withFreshUserPrefs { reader -> reader.authTokenFlow.first() }
        assertEquals("survives-restart", readBack)
    }
}
