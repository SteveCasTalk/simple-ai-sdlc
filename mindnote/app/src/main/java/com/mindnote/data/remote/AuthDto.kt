package com.mindnote.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class AccountDto(val id: String, val username: String)

@Serializable
data class AuthSuccessDto(val token: String, val account: AccountDto)

@Serializable
data class AuthErrorDto(val code: String, val message: String)

@Serializable
data class AuthErrorEnvelopeDto(val error: AuthErrorDto)
