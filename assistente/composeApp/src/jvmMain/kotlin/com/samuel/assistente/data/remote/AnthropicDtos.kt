package com.samuel.assistente.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    /** Mensagens em JSON cru: o conteúdo do assistente é reenviado exatamente como veio. */
    val messages: List<JsonObject>,
    @SerialName("output_config") val outputConfig: OutputConfig,
    val fallbacks: String = "default",
)

@Serializable
data class OutputConfig(val effort: String)

@Serializable
data class MessagesResponse(
    val id: String,
    val model: String,
    /** Mantido como JSON cru para ser reenviado sem alterações no próximo turno. */
    val content: JsonArray,
    @SerialName("stop_reason") val stopReason: String? = null,
)

@Serializable
data class ApiErrorEnvelope(val error: ApiError? = null)

@Serializable
data class ApiError(val type: String? = null, val message: String? = null)
