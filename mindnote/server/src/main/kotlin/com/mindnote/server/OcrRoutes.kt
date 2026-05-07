package com.mindnote.server

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("OcrRoutes")

const val DEFAULT_MAX_IMAGE_SIZE: Long = 25L * 1024L * 1024L
private val ALLOWED_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val PROVIDER_TIMEOUT_MS = 50_000L

fun Route.ocrRoutes(provider: OcrProvider, maxImageSize: Long = DEFAULT_MAX_IMAGE_SIZE) {
    post("/ocr") {
        call.userIdFromHeader()

        val multipart = call.receiveMultipart()
        var imagePart: PartData.FileItem? = null
        multipart.forEachPart { part ->
            if (part is PartData.FileItem && part.name == "image" && imagePart == null) {
                imagePart = part
            } else {
                part.dispose()
            }
        }
        val part = imagePart
        if (part == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorEnvelope(ErrorBody("bad_request", "missing image part")),
            )
            return@post
        }
        try {
            val ct = part.contentType
            val ctString = ct?.let { "${it.contentType}/${it.contentSubtype}" }?.lowercase().orEmpty()
            if (ctString !in ALLOWED_TYPES) {
                call.respond(
                    HttpStatusCode.UnsupportedMediaType,
                    ErrorEnvelope(ErrorBody("unsupported_media", "content type '$ctString' not allowed")),
                )
                return@post
            }
            val bytes = part.provider().toByteArray()
            if (bytes.size.toLong() > maxImageSize) {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    ErrorEnvelope(ErrorBody("image_too_large", "image exceeds size cap")),
                )
                return@post
            }
            val result = try {
                withTimeout(PROVIDER_TIMEOUT_MS) { provider.extractText(bytes, ctString) }
            } catch (_: TimeoutCancellationException) {
                call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorEnvelope(ErrorBody("ocr_provider_error", "upstream timeout")),
                )
                return@post
            } catch (e: OcrException.Empty) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorEnvelope(ErrorBody("ocr_empty", "no text extracted")),
                )
                return@post
            } catch (e: OcrException.Upstream) {
                log.warn("ocr provider error status={} msg={}", e.status, e.message)
                call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorEnvelope(ErrorBody("ocr_provider_error", "upstream ${e.status}")),
                )
                return@post
            }
            if (result.text.isBlank()) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    ErrorEnvelope(ErrorBody("ocr_empty", "no text extracted")),
                )
                return@post
            }
            call.respond(OcrResponseDto(text = result.text, languageHint = result.languageHint))
        } finally {
            part.dispose()
        }
    }
}
