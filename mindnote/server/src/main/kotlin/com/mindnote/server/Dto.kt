package com.mindnote.server

import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val preview: String,
    val body: String,
    val tags: List<String>,
    val date: String, // ISO-8601 LocalDate (yyyy-MM-dd)
)

@Serializable
data class NoteCreateDto(
    val id: String? = null,
    val title: String,
    val body: String,
    val preview: String = "",
    val tags: List<String> = emptyList(),
    val date: String? = null,
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)

@Serializable
data class SendMessageDto(val text: String)

@Serializable
data class LatestConversationDto(val id: String? = null)

@Serializable
data class OcrResponseDto(
    val text: String,
    val languageHint: String = "",
)

@Serializable
data class ErrorEnvelope(val error: ErrorBody)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val fields: Map<String, String>? = null,
)

// --- Auth ---

@Serializable
data class RegisterRequestDto(val username: String, val password: String)

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class AccountDto(val id: String, val username: String)

@Serializable
data class AuthSuccessDto(val token: String, val account: AccountDto)
