package com.mindnote.server

import de.mkammerer.argon2.Argon2Factory

/**
 * Argon2id password hashing helpers (per Decision D1).
 *
 * Parameters: t=3, m=65536 (64 MiB), p=4 — modern OWASP-recommended defaults for Argon2id.
 * Encoded output (~96 chars) easily fits in the auth_accounts.password_hash varchar(256).
 */
object Passwords {
    private const val ITERATIONS = 3
    private const val MEMORY_KIB = 65_536
    private const val PARALLELISM = 4

    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun hash(password: String): String {
        val chars = password.toCharArray()
        return try {
            argon2.hash(ITERATIONS, MEMORY_KIB, PARALLELISM, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }

    fun verify(encodedHash: String, password: String): Boolean {
        val chars = password.toCharArray()
        return try {
            argon2.verify(encodedHash, chars)
        } finally {
            argon2.wipeArray(chars)
        }
    }
}
