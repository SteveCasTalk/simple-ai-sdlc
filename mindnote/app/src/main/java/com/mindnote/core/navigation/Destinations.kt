package com.mindnote.core.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val Onboarding = "onboarding"
    const val Login = "login"
    const val Home = "home"
    const val Chat = "chat/{conversationId}?text={text}"
    const val Notes = "notes"
    const val Capture = "capture"
    const val Scan = "scan"
    const val NoteDetail = "note/{noteId}"

    fun chat(id: String, text: String? = null): String {
        val base = "chat/$id"
        return if (text.isNullOrBlank()) base
        else "$base?text=${URLEncoder.encode(text, StandardCharsets.UTF_8.name())}"
    }

    fun noteDetail(id: String) = "note/$id"
}

/**
 * Decide which screen the app should land on at cold start.
 *
 * - Signed-in users (token present) go straight to [Routes.Home] regardless of the
 *   `onboarded` flag — a stored token means they've already signed up and onboarding
 *   would be a re-prompt.
 * - Signed-out users go to [Routes.Login]. The onboarded flag is a leftover from the
 *   pre-auth single-user model; once auth is fully rolled out it can be retired.
 */
fun decideStartDestination(hasToken: Boolean, isOnboarded: Boolean): String =
    if (hasToken) Routes.Home else Routes.Login
