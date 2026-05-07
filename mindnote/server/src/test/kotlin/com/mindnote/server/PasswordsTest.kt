package com.mindnote.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordsTest {

    @Test
    fun `hash produces an argon2id-encoded string`() {
        val hash = Passwords.hash("Hunter2!aB")
        assertTrue(hash.startsWith("\$argon2id\$"), "hash should start with argon2id marker, was: $hash")
    }

    @Test
    fun `hash of same password twice yields different strings (salted)`() {
        val a = Passwords.hash("Hunter2!aB")
        val b = Passwords.hash("Hunter2!aB")
        assertNotEquals(a, b)
    }

    @Test
    fun `verify returns true for the correct password`() {
        val hash = Passwords.hash("Hunter2!aB")
        assertTrue(Passwords.verify(hash, "Hunter2!aB"))
    }

    @Test
    fun `verify returns false for the wrong password`() {
        val hash = Passwords.hash("Hunter2!aB")
        assertFalse(Passwords.verify(hash, "wrong-password"))
    }

    @Test
    fun `verify returns false for a tampered hash`() {
        val hash = Passwords.hash("Hunter2!aB")
        val tampered = hash.dropLast(4) + "AAAA"
        assertFalse(Passwords.verify(tampered, "Hunter2!aB"))
    }

    @Test
    fun `hash fits in the password_hash column (256 chars)`() {
        val hash = Passwords.hash("Hunter2!aB")
        assertTrue(hash.length <= 256, "hash too long for column: ${hash.length}")
    }
}
