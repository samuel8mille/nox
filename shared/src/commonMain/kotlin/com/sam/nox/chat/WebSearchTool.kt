package com.sam.nox.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// --- Protocolo da API de busca Tavily (https://tavily.com) ---

@Serializable
private data class TavilyRequest(
    val query: String,
    val search_depth: String = "basic",
    val include_answer: Boolean = true,
    val max_results: Int = 5,
)

@Serializable
private data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = "",
)

@Serializable
private data class TavilyResponse(
    val answer: String? = null,
    val results: List<TavilyResult> = emptyList(),
)

/**
 * Cria a ferramenta de busca web via Tavily — uma API de busca feita para LLMs,
 * que devolve resultados atuais já resumidos (notícias, eventos, preços, etc.).
 *
 * Requer uma API key (criada em https://tavily.com, free tier de 1000 buscas/mês),
 * lida por padrão de TAVILY_API_KEY / -Dtavily.key.
 */
fun webSearchTool(
    apiKey: String,
    httpClient: HttpClient = createHttpClient(),
): Tool {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    return Tool(
        name = "pesquisar_web",
        description = "Pesquisa na web informações atuais: notícias, eventos, jogos, " +
            "datas, preços, pessoas, lugares e qualquer fato que possa mudar com o tempo. " +
            "Use sempre que a resposta puder depender de informação recente ou que você " +
            "não saiba com certeza.",
        parameters = JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
                "properties" to JsonObject(
                    mapOf(
                        "consulta" to JsonObject(
                            mapOf(
                                "type" to JsonPrimitive("string"),
                                "description" to JsonPrimitive("O termo ou pergunta a pesquisar."),
                            ),
                        ),
                    ),
                ),
                "required" to kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("consulta"))),
            ),
        ),
    ) { args ->
        val query = (args["consulta"] as? JsonPrimitive)?.content?.trim().orEmpty()
        if (query.isEmpty()) return@Tool "Erro: consulta vazia."

        try {
            val raw = httpClient.post("https://api.tavily.com/search") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(json.encodeToString(TavilyRequest(query = query)))
            }.bodyAsText()

            val resp = json.decodeFromString<TavilyResponse>(raw)
            formatResult(resp)
        } catch (e: Throwable) {
            "Erro ao pesquisar: ${e.message}"
        }
    }
}

/** Monta um resumo textual dos resultados da Tavily para devolver ao modelo. */
private fun formatResult(resp: TavilyResponse): String = buildString {
    // A Tavily já devolve uma resposta sintetizada (quando include_answer=true).
    if (!resp.answer.isNullOrBlank()) {
        appendLine("Resumo: ${resp.answer.trim()}")
        appendLine()
    }
    val results = resp.results.take(5)
    if (results.isNotEmpty()) {
        appendLine("Fontes:")
        results.forEach { r ->
            val snippet = r.content.trim().take(300)
            appendLine("- ${r.title}: $snippet (${r.url})")
        }
    }
}.trim().ifEmpty { "Nenhum resultado encontrado para a consulta." }
