package com.mindnote.server

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class OcrRoutesTest {

    private val tinyJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    private fun setup(provider: OcrProvider) = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing { ocrRoutes(provider) }
        }
    }

    @Test
    fun `200 OK returns text and languageHint from provider`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        OcrResult(text = "hello", languageHint = "en")
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tinyJpegBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"x.jpg\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body: OcrResponseDto = response.body()
        assertEquals("hello", body.text)
        assertEquals("en", body.languageHint)
    }

    @Test
    fun `400 bad_request when image part is missing`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        error("provider should not be called")
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("not_image", "anything")
            }))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("bad_request", env.error.code)
    }

    @Test
    fun `415 unsupported_media when content type is not allowed`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        error("provider should not be called")
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tinyJpegBytes, Headers.build {
                    append(HttpHeaders.ContentType, "application/pdf")
                    append(HttpHeaders.ContentDisposition, "filename=\"x.pdf\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("unsupported_media", env.error.code)
    }

    @Test
    fun `413 image_too_large when image exceeds the cap`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(
                    provider = object : OcrProvider {
                        override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                            error("provider should not be called, got ${bytes.size} bytes")
                    },
                    maxImageSize = 10_000L,
                )
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val tooBig = ByteArray(100_000) { 1 }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tooBig, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"big.jpg\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("image_too_large", env.error.code)
    }

    @Test
    fun `422 ocr_empty when provider throws Empty`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        throw OcrException.Empty()
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tinyJpegBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"x.jpg\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("ocr_empty", env.error.code)
    }

    @Test
    fun `422 ocr_empty when provider returns blank text`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        OcrResult(text = "   \n\t ", languageHint = "")
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tinyJpegBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"x.jpg\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("ocr_empty", env.error.code)
    }

    @Test
    fun `502 ocr_provider_error when provider throws Upstream`() = testApplication {
        application {
            install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
            routing {
                ocrRoutes(object : OcrProvider {
                    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult =
                        throw OcrException.Upstream(503, "down")
                })
            }
        }
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = client.post("/ocr") {
            setBody(MultiPartFormDataContent(formData {
                append("image", tinyJpegBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"x.jpg\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.BadGateway, response.status)
        val env: ErrorEnvelope = response.body()
        assertEquals("ocr_provider_error", env.error.code)
    }
}
