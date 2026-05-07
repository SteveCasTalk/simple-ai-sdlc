package com.mindnote.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthValidationTest {

    // ----- username -----

    @Test
    fun `username valid - lowercase letters digits and underscore`() {
        assertNull(validateUsername("alice_42"))
        assertNull(validateUsername("aaa"))                          // min length
        assertNull(validateUsername("a".repeat(30)))                 // max length
        assertNull(validateUsername("user_name_with_underscores_99")) // 30 chars
    }

    @Test
    fun `username invalid - too short`() {
        assertNotNull(validateUsername("ab"))
        assertNotNull(validateUsername(""))
    }

    @Test
    fun `username invalid - too long`() {
        assertNotNull(validateUsername("a".repeat(31)))
    }

    @Test
    fun `username invalid - uppercase letters`() {
        assertNotNull(validateUsername("Alice42"))
    }

    @Test
    fun `username invalid - spaces`() {
        assertNotNull(validateUsername("alice 42"))
    }

    @Test
    fun `username invalid - other special chars`() {
        assertNotNull(validateUsername("alice-42"))
        assertNotNull(validateUsername("alice.42"))
        assertNotNull(validateUsername("alice@42"))
    }

    // ----- password -----

    @Test
    fun `password valid - meets all five rules`() {
        assertNull(validatePassword("Hunter2!"))      // 8 chars: upper, lower, digit, special
        assertNull(validatePassword("Aa1!aaaa"))
        assertNull(validatePassword("My_long_password_123!"))
    }

    @Test
    fun `password invalid - too short`() {
        assertNotNull(validatePassword("Aa1!"))
        assertNotNull(validatePassword(""))
    }

    @Test
    fun `password invalid - missing uppercase`() {
        assertNotNull(validatePassword("hunter2!aaaa"))
    }

    @Test
    fun `password invalid - missing lowercase`() {
        assertNotNull(validatePassword("HUNTER2!AAAA"))
    }

    @Test
    fun `password invalid - missing digit`() {
        assertNotNull(validatePassword("Hunters!aaa"))
    }

    @Test
    fun `password invalid - missing special char`() {
        assertNotNull(validatePassword("Hunter22aaaa"))
    }

    @Test
    fun `password validation message names the violated rule`() {
        // We don't lock exact wording, but it should mention what's missing.
        val msg = validatePassword("hunter2!aaaa") ?: error("expected failure")
        assertEquals(true, msg.contains("uppercase", ignoreCase = true), "msg was: $msg")
    }
}
