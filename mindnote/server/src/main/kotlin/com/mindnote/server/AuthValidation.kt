package com.mindnote.server

private val USERNAME_RE = Regex("^[a-z0-9_]+$")
private val SPECIAL_RE = Regex("""[!@#${'$'}%^&*()_+\-=\[\]{};:'",.<>/?\\|~`]""")

/**
 * Validate a username per the api-contract: 3–30 chars, lowercase letters / digits / underscore.
 * Returns null if valid, or a human-readable rule violation otherwise.
 */
fun validateUsername(username: String): String? = when {
    username.length < 3 -> "must be at least 3 characters"
    username.length > 30 -> "must be at most 30 characters"
    !USERNAME_RE.matches(username) -> "must contain only lowercase letters, digits, and underscore"
    else -> null
}

/**
 * Validate a password per the api-contract: length ≥ 8, ≥1 uppercase, ≥1 lowercase, ≥1 digit,
 * ≥1 special character. Returns null if valid, or a human-readable rule violation otherwise.
 */
fun validatePassword(password: String): String? = when {
    password.length < 8 -> "must be at least 8 characters"
    password.none { it.isUpperCase() } -> "must contain an uppercase letter"
    password.none { it.isLowerCase() } -> "must contain a lowercase letter"
    password.none { it.isDigit() } -> "must contain a digit"
    !SPECIAL_RE.containsMatchIn(password) -> "must contain a special character"
    else -> null
}
