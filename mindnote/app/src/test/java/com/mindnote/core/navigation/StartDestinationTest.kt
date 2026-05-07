package com.mindnote.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    @Test
    fun `signed in and onboarded routes to Home`() {
        assertEquals(Routes.Home, decideStartDestination(hasToken = true, isOnboarded = true))
    }

    @Test
    fun `signed in but not onboarded routes to still Home (signed in users skip onboarding)`() {
        // Existing user re-installing on a new device etc. — onboarded flag is per-install, but
        // a stored token means they've already signed up before; no point gating them with
        // onboarding again.
        assertEquals(Routes.Home, decideStartDestination(hasToken = true, isOnboarded = false))
    }

    @Test
    fun `signed out and onboarded routes to Login (returning user, just logged out)`() {
        assertEquals(Routes.Login, decideStartDestination(hasToken = false, isOnboarded = true))
    }

    @Test
    fun `signed out and not onboarded routes to Login (first launch, must auth before app)`() {
        assertEquals(Routes.Login, decideStartDestination(hasToken = false, isOnboarded = false))
    }
}
