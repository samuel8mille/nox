package com.samuel.assistente.data

import com.samuel.assistente.data.remote.AnthropicApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

interface ChatRepository {
    /** Envia [text] com todo o histórico e devolve o texto da resposta. Lança [ChatError]. */
    suspend fun send(text: String): String

    fun clear()
}

/**
 * Guarda o histórico no formato da API. Os blocos do assistente (inclusive blocos de
 * raciocínio) são reenviados sem alteração, como a API exige. Uma troca só entra no
 * histórico quando dá certo, então uma falha pode ser reenviada sem duplicar mensagens.
 */
class AnthropicChatRepository(private val api: AnthropicApi) : ChatRepository {

    private val mutex = Mutex()
    private val history = mutableListOf<JsonObject>()

    override suspend fun send(text: String): String = mutex.withLock {
        val userMessage = buildJsonObject {
            put("role", "user")
            put("content", text)
        }

        val response = api.createMessage(history + userMessage)

        if (response.stopReason == "refusal") throw ChatError.Refused()

        val reply = response.content
            .map { it.jsonObject }
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            .ifBlank { "(resposta vazia)" }

        history += userMessage
        history += JsonObject(
            mapOf(
                "role" to JsonPrimitive("assistant"),
                "content" to response.content,
            )
        )
        reply
    }

    override fun clear() {
        history.clear()
    }
}
