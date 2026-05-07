package com.mindnote.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MindNoteServer")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    Database.init()

    val chatProvider: ChatProvider = AnthropicChatProvider(
        apiKey = System.getenv("ANTHROPIC_API_KEY")
            ?: error("ANTHROPIC_API_KEY not set"),
    )

    val ocrProvider: OcrProvider = installOcr()

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(SSE)
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "error")))
        }
    }

    routing {
        get("/health") { call.respondText("ok") }
        authRoutes()
        notesRoutes()
        favoritesRoutes()
        chatRoutes(chatProvider)
        ocrRoutes(ocrProvider)
    }
}
