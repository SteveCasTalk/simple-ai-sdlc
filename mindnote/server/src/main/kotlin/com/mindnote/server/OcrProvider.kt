package com.mindnote.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.Base64

interface OcrProvider {
    suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult
}

data class OcrResult(val text: String, val languageHint: String)

sealed class OcrException(msg: String) : RuntimeException(msg) {
    class Empty : OcrException("provider returned no text")
    class Upstream(val status: Int, msg: String) : OcrException(msg)
}

private val log = LoggerFactory.getLogger("OcrProvider")

class GoogleVisionOcrProvider(private val apiKey: String) : OcrProvider {

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class VisionImage(val content: String)

    @Serializable
    private data class VisionFeature(val type: String, val maxResults: Int = 1)

    @Serializable
    private data class VisionRequest(val image: VisionImage, val features: List<VisionFeature>)

    @Serializable
    private data class VisionEnvelope(val requests: List<VisionRequest>)

    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult {
        val body = VisionEnvelope(
            requests = listOf(
                VisionRequest(
                    image = VisionImage(content = Base64.getEncoder().encodeToString(bytes)),
                    features = listOf(VisionFeature(type = "TEXT_DETECTION")),
                ),
            ),
        )
        val response = client.post("https://vision.googleapis.com/v1/images:annotate?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!response.status.isSuccess()) {
            val errBody = runCatching { response.body<String>() }.getOrDefault("")
            throw OcrException.Upstream(response.status.value, "vision: ${response.status.value} $errBody")
        }
        val payload = response.body<String>()
        val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrElse {
            throw OcrException.Upstream(response.status.value, "vision: malformed JSON")
        }
        val first = root["responses"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw OcrException.Empty()
        val text = first["fullTextAnnotation"]?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: first.firstAnnotationText()
            ?: throw OcrException.Empty()
        if (text.isBlank()) throw OcrException.Empty()
        val locale = first.firstAnnotationLocale().orEmpty()
        return OcrResult(text = text.trim(), languageHint = locale)
    }

    private fun JsonObject.firstAnnotationText(): String? =
        this["textAnnotations"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("description")?.jsonPrimitive?.content

    private fun JsonObject.firstAnnotationLocale(): String? =
        this["textAnnotations"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("locale")?.jsonPrimitive?.content
}

class StubOcrProvider : OcrProvider {
    override suspend fun extractText(bytes: ByteArray, contentType: String): OcrResult {
        throw OcrException.Upstream(503, "no provider configured")
    }
}

fun installOcr(): OcrProvider {
    val key = System.getenv("OCR_API_KEY")
    return if (key.isNullOrBlank()) {
        log.warn("OCR_API_KEY not set — falling back to StubOcrProvider; /ocr will return 503")
        StubOcrProvider()
    } else {
        GoogleVisionOcrProvider(apiKey = key)
    }
}

private fun io.ktor.http.HttpStatusCode.isSuccess(): Boolean = value in 200..299
