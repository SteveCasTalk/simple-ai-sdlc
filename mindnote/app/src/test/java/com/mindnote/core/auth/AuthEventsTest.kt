package com.mindnote.core.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthEventsTest {

    @Test
    fun `subscribers receive signedOut emissions`() = runTest {
        val events = AuthEvents()
        val received = mutableListOf<Unit>()
        val job = launch { events.signedOut.collect { received.add(it) } }
        advanceUntilIdle() // let the collector subscribe before we emit

        events.emitSignedOut()
        advanceUntilIdle()

        assertEquals(1, received.size)
        job.cancel()
    }

    @Test
    fun `multiple emissions are all delivered`() = runTest {
        val events = AuthEvents()
        val received = mutableListOf<Unit>()
        val job = launch { events.signedOut.collect { received.add(it) } }
        advanceUntilIdle()

        events.emitSignedOut()
        events.emitSignedOut()
        events.emitSignedOut()
        advanceUntilIdle()

        assertEquals(3, received.size)
        job.cancel()
    }
}
