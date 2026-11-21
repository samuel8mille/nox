package com.sam.nox.chat

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Cria o HttpClient com o engine adequado a cada plataforma (OkHttp no JVM/Android, Darwin no iOS). */
expect fun createHttpClient(): HttpClient

/**
 * HttpClient configurado para o cliente de LLM: negociação de conteúdo JSON (tolerante a
 * chaves desconhecidas) e timeouts generosos. Centraliza a configuração HTTP para o
 * [OpenRouterClient] recebê-la pronta, em vez de a montar internamente (DIP).
 */
fun openRouterHttpClient(): HttpClient = createHttpClient().config {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 120_000
    }
}
