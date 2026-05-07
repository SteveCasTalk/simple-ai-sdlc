package com.mindnote.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `OcrResponseDto deserializes text and languageHint`() {
        val parsed = json.decodeFromString<OcrResponseDto>(
            """{"text":"hello","languageHint":"en"}"""
        )
        assertEquals("hello", parsed.text)
        assertEquals("en", parsed.languageHint)
    }

    @Test
    fun `OcrResponseDto languageHint defaults to empty string when absent`() {
        val parsed = json.decodeFromString<OcrResponseDto>(
            """{"text":"only text"}"""
        )
        assertEquals("only text", parsed.text)
        assertEquals("", parsed.languageHint)
    }
}
