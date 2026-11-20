package com.samuel.assistente.data.remote

import com.samuel.assistente.data.ChatError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.IOException

/** Cliente HTTP fino para POST /v1/messages. Não guarda estado de conversa. */
class AnthropicApi(
    private val apiKeyProvider: () -> String? = { System.getenv(AnthropicConfig.API_KEY_ENV) },
) : AutoCloseable {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            // Respostas longas com raciocínio podem levar alguns minutos.
            requestTimeoutMillis = 5 * 60_000
            socketTimeoutMillis = 5 * 60_000
        }
    }

    /** Lança [ChatError] em qualquer falha. */
    suspend fun createMessage(messages: List<JsonObject>): MessagesResponse {
        val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() } ?: throw ChatError.MissingApiKey()

        val response = try {
            http.post(AnthropicConfig.API_URL) {
                header("x-api-key", apiKey)
                header("anthropic-version", AnthropicConfig.API_VERSION)
                header("anthropic-beta", AnthropicConfig.FALLBACK_BETA)
                contentType(ContentType.Application.Json)
                setBody(
                    MessagesRequest(
                        model = AnthropicConfig.MODEL,
                        maxTokens = AnthropicConfig.MAX_TOKENS,
                        system = AnthropicConfig.SYSTEM_PROMPT,
                        messages = messages,
                        outputConfig = OutputConfig(effort = AnthropicConfig.EFFORT),
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ChatError.Network(e)
        } catch (e: Exception) {
            throw ChatError.Unexpected(e)
        }

        val status = response.status.value
        if (!response.status.isSuccess()) {
            val detail = runCatching {
                json.decodeFromString(ApiErrorEnvelope.serializer(), response.bodyAsText()).error?.message
            }.getOrNull()
            throw when (status) {
                401, 403 -> ChatError.InvalidApiKey(detail)
                429 -> ChatError.RateLimited()
                500, 502, 503, 504, 529 -> ChatError.Overloaded(status)
                else -> ChatError.Api(status, detail)
            }
        }

        return try {
            response.body<MessagesResponse>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ChatError.Unexpected(e)
        }
    }

    override fun close() = http.close()
}
