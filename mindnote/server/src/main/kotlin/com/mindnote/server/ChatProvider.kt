package com.mindnote.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

data class ChatTurn(val role: String, val content: String)

interface ChatProvider {
    fun stream(system: String, turns: List<ChatTurn>): Flow<String>
}

private val log = LoggerFactory.getLogger("ChatProvider")

class AnthropicChatProvider(
    private val apiKey: String,
    private val model: String = "claude-haiku-4-5-20251001",
    private val maxTokens: Int = 1024,
) : ChatProvider {

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 120_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class AnthropicMessage(val role: String, val content: String)

    @Serializable
    private data class AnthropicRequest(
        val model: String,
        @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int,
        val system: String,
        val messages: List<AnthropicMessage>,
        val stream: Boolean = true,
    )

    override fun stream(system: String, turns: List<ChatTurn>): Flow<String> = channelFlow {
        val request = AnthropicRequest(
            model = model,
            maxTokens = maxTokens,
            system = system,
            messages = turns.map { AnthropicMessage(it.role, it.content) },
        )
        client.preparePost("https://api.anthropic.com/v1/messages") {
            headers {
                append("x-api-key", apiKey)
                append("anthropic-version", "2023-06-01")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute { response ->
            log.info("anthropic response status={}", response.status)
            val channel = response.bodyAsChannel()
            var lineCount = 0
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                lineCount++
                if (lineCount <= 6) log.info("anthropic line[{}]: {}", lineCount, line.take(200))
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue
                val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: continue
                val type = obj["type"]?.jsonPrimitive?.content
                if (type == "content_block_delta") {
                    val deltaText = obj["delta"]
                        ?.jsonObject
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.content
                    if (!deltaText.isNullOrEmpty()) send(deltaText)
                }
            }
            log.info("anthropic stream done, lines={}", lineCount)
        }
    }
}
