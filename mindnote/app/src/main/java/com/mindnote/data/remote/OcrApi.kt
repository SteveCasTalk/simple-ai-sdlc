package com.mindnote.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class OcrResponseDto(
    val text: String,
    val languageHint: String = "",
)

class OcrApi(private val client: HttpClient) {
    suspend fun ocr(
        deviceId: String,
        imageBytes: ByteArray,
        contentType: String = "image/jpeg",
    ): OcrResponseDto =
        client.post("ocr") {
            header("X-Device-Id", deviceId)
            setBody(MultiPartFormDataContent(formData {
                append("image", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                })
            }))
        }.body()
}
