package com.sam.nox.chat

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// ----------------------------------------------------------------------------
// Modelo do protocolo OpenAI-compatível (usado pelo OpenRouter).
// Doc: https://openrouter.ai/docs
// ----------------------------------------------------------------------------

@Serializable
private data class ORMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ORToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null,
)

@Serializable
private data class ORToolCall(
    val id: String = "",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String = "function",
    val function: ORToolCallFunction,
)

@Serializable
private data class ORToolCallFunction(
    val name: String,
    /** Argumentos vêm/vão como string JSON (formato OpenAI). */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val arguments: String = "{}",
)

@Serializable
private data class ORToolSpec(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String = "function",
    val function: ORFunctionSpec,
)

@Serializable
private data class ORFunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

@Serializable
private data class ORRequest(
    val model: String,
    val messages: List<ORMessage>,
    val tools: List<ORToolSpec>? = null,
)

@Serializable
private data class ORChoice(
    val message: ORMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
private data class ORError(
    val message: String? = null,
    val code: Int? = null,
)

/** Mensagem amigável a partir do erro do OpenRouter (desembrulha 429, etc.). */
private fun ORError.friendly(): String = when (code) {
    429 -> "Limite de uso gratuito atingido. Aguarde um instante e tente de novo."
    402 -> "Créditos insuficientes no OpenRouter para este modelo."
    401, 403 -> "Chave do OpenRouter inválida ou sem permissão."
    502, 503 -> "O provedor do modelo está instável agora. Tente de novo."
    else -> message ?: "Falha no OpenRouter."
}

@Serializable
private data class ORResponse(
    val choices: List<ORChoice> = emptyList(),
    val error: ORError? = null,
)

/**
 * Cliente da conversa (LLM) via OpenRouter — endpoint compatível com OpenAI que dá
 * acesso a vários modelos, incluindo opções gratuitas (sufixo `:free`).
 *
 * Requer uma API key criada em https://openrouter.ai/keys. Suporta tool calling.
 * A busca web é fornecida como uma ferramenta comum (ver [webSearchTool]).
 *
 * @param model id do modelo (ex.: "nvidia/nemotron-3.5-lightning:free").
 */
class OpenRouterClient(
    private val apiKey: String,
    val model: String = "nvidia/nemotron-3.5-super-vl:free",
    private val systemPrompt: String? = defaultSystemPrompt,
    private val tools: List<Tool> = emptyList(),
    /**
     * Modelos alternativos (que fazem tool calling em JSON, formato padrão),
     * tentados em ordem se o principal falhar — dá resiliência a provedores instáveis.
     */
    private val fallbackModels: List<String> = listOf(
        "nvidia/nemotron-3-ultra-550b-a55b:free",
    ),
    /**
     * Pedido de confirmação para ferramentas que alteram algo externo. Recebe o
     * texto de preview da ação e devolve true (aprovar) ou false (recusar).
     * Por padrão aprova tudo (sem confirmação).
     */
    private val confirm: suspend (preview: String) -> Boolean = { true },
    /**
     * Contexto extra injetado no system prompt a cada mensagem (ao lado da data/hora).
     * Usado, por exemplo, para os fatos lembrados sobre o usuário. Vazio = nada a acrescentar.
     */
    private val extraContext: () -> String = { "" },
    private val httpClient: HttpClient = createHttpClient(),
    private val baseUrl: String = "https://openrouter.ai/api/v1",
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val client = httpClient.config {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
    }

    private val toolsByName = tools.associateBy { it.name }

    /**
     * Especificações das ferramentas a enviar ao modelo nesta requisição. Seleciona
     * dinamicamente um subconjunto relevante a [forText] (ver [ToolSelector]) para
     * respeitar o limite de ferramentas do provedor. Null quando não há ferramentas.
     */
    private fun toolSpecsFor(forText: String): List<ORToolSpec>? = tools
        .takeIf { it.isNotEmpty() }
        ?.let { ToolSelector.select(it, forText) }
        ?.map { ORToolSpec(function = ORFunctionSpec(it.name, it.description, it.parameters)) }

    /** Texto recente (últimas mensagens do usuário) que guia a seleção de ferramentas. */
    private fun recentUserText(): String =
        history.filter { it.role == "user" }
            .takeLast(2)
            .joinToString(" ") { it.content.orEmpty() }

    private val history = mutableListOf<ORMessage>()

    /** Envia a mensagem do usuário e devolve a resposta da assistente. */
    suspend fun send(userMessage: String): String {
        history.add(ORMessage(role = "user", content = userMessage))
        return runConversation()
    }

    private suspend fun runConversation(maxToolRounds: Int = 5): String {
        repeat(maxToolRounds + 1) {
            val message = requestOnce()
            history.add(message)

            val calls = message.toolCalls
            if (calls.isNullOrEmpty()) {
                val answer = message.content?.trim().orEmpty()
                // Alguns modelos escrevem a chamada de função como TEXTO em vez de
                // usar o campo tool_calls. Isso é inútil para nós → aciona fallback.
                if (looksLikeTextToolCall(answer)) {
                    throw OpenRouterException("O modelo não chamou a ferramenta corretamente.")
                }
                if (answer.isNotEmpty()) return answer
                throw OpenRouterException("O modelo não retornou texto. Tente reformular a pergunta.")
            }

            // Executa cada ferramenta pedida e devolve o resultado (role=tool).
            for (call in calls) {
                val result = executeTool(call.function)
                history.add(
                    ORMessage(
                        role = "tool",
                        toolCallId = call.id,
                        name = call.function.name,
                        content = result,
                    ),
                )
            }
        }
        return history.lastOrNull { it.role == "assistant" }?.content?.trim().orEmpty()
    }

    private suspend fun requestOnce(): ORMessage {
        // Tenta o modelo principal e, se falhar (provedor instável), cai para os
        // fallbacks em ordem. Só propaga o erro se todos falharem.
        val candidates = listOf(model) + fallbackModels
        var lastError: OpenRouterException? = null
        for (m in candidates) {
            try {
                return requestModel(m)
            } catch (e: OpenRouterException) {
                lastError = e
            }
        }
        throw lastError ?: OpenRouterException("Falha ao consultar o modelo.")
    }

    private suspend fun requestModel(modelId: String): ORMessage {
        val messages = buildList {
            if (systemPrompt != null) {
                val extra = extraContext().trim().let { if (it.isEmpty()) "" else "\n\n$it" }
                add(ORMessage(role = "system", content = currentContext() + extra + "\n\n" + systemPrompt))
            }
            addAll(history)
        }
        val tools = toolSpecsFor(recentUserText())
        val body = json.encodeToString(ORRequest(model = modelId, messages = messages, tools = tools))

        val text = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(body)
        }.bodyAsText()
        val response = json.decodeFromString<ORResponse>(text)
        response.error?.let { throw OpenRouterException(it.friendly()) }
        val msg = response.choices.firstOrNull()?.message
        // Resposta vazia (sem texto nem tool_calls) conta como falha → tenta fallback.
        if (msg == null || (msg.content.isNullOrBlank() && msg.toolCalls.isNullOrEmpty())) {
            throw OpenRouterException("O modelo não retornou conteúdo.")
        }
        return msg
    }

    private suspend fun executeTool(function: ORToolCallFunction): String {
        val tool = toolsByName[function.name]
            ?: return "Erro: ferramenta '${function.name}' não existe."
        val args = runCatching {
            json.decodeFromString<JsonObject>(function.arguments)
        }.getOrDefault(JsonObject(emptyMap()))

        // Ações que alteram algo externo exigem confirmação do usuário.
        if (tool.requiresConfirmation) {
            val preview = tool.preview?.invoke(args) ?: tool.description
            if (!confirm(preview)) {
                return "O usuário RECUSOU esta ação. Não a execute e pergunte como prosseguir."
            }
        }

        return try {
            tool.execute(args)
        } catch (e: Throwable) {
            "Erro ao executar '${function.name}': ${e.message}"
        }
    }

    /** Limpa o histórico. */
    fun reset() {
        history.clear()
    }

    fun close() = client.close()

    companion object {
        private fun currentContext(): String {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val dia = when (now.dayOfWeek) {
                DayOfWeek.MONDAY -> "segunda-feira"
                DayOfWeek.TUESDAY -> "terça-feira"
                DayOfWeek.WEDNESDAY -> "quarta-feira"
                DayOfWeek.THURSDAY -> "quinta-feira"
                DayOfWeek.FRIDAY -> "sexta-feira"
                DayOfWeek.SATURDAY -> "sábado"
                DayOfWeek.SUNDAY -> "domingo"
                else -> ""
            }
            val h = now.hour.toString().padStart(2, '0')
            val m = now.minute.toString().padStart(2, '0')
            return "Contexto: agora é $dia, ${now.dayOfMonth}/${now.monthNumber}/${now.year}, $h:$m."
        }

        private val defaultSystemPrompt = """
            Você é a Nox, uma assistente útil que responde em português de forma clara e concisa.

            Você tem uma ferramenta de pesquisa na web. Para perguntas sobre fatos, pessoas,
            lugares, eventos ou qualquer coisa que você não saiba com certeza, use a ferramenta
            de pesquisa antes de responder — não invente nem diga que não tem acesso à internet.

            Você tem memória de longo prazo. SEMPRE que o usuário revelar um fato sobre si — o nome,
            uma preferência, um projeto, onde mora, o que faz — chame a ferramenta lembrar IMEDIATAMENTE
            para guardar esse fato, antes ou junto da sua resposta. Por exemplo, se ele disser "meu
            nome é X" ou "eu trabalho com Y", chame lembrar com esse fato. Quando ele pedir para ver
            o que você sabe, use listar_memorias; para esquecer algo, use esquecer. Os fatos que você
            já guardou aparecem no seu contexto.

            Você também pode executar ações no computador do usuário através de ferramentas:
            rodar comandos no terminal (git, scripts), criar e ler arquivos, listar pastas, e
            agir no GitHub (criar repositório, issue, pull request). Para tarefas como "faça
            commit", "crie um README", "dê push", USE essas ferramentas — você É capaz de fazer
            isso. Antes de mexer numa pasta, defina a pasta de trabalho com a ferramenta própria,
            pedindo o caminho ao usuário se ele não informou. Ações que alteram arquivos, o
            sistema ou o GitHub pedem confirmação ao usuário automaticamente.

            Use ferramentas apenas quando forem realmente necessárias. Para saudações e conversa
            casual, responda normalmente, sem pesquisar.

            Responda em texto simples, sem formatação Markdown. Não use asteriscos para negrito
            ou itálico (**, *), nem títulos (#), nem marcadores de código (`).
        """.trimIndent()
    }
}

/** Detecta uma tool call escrita como texto (formato não-padrão de alguns modelos). */
private fun looksLikeTextToolCall(text: String): Boolean {
    val t = text.lowercase()
    return ("function_call" in t && ("<invoke" in t || "name=" in t)) ||
        (t.startsWith("<") && "invoke name=" in t)
}

/** Erro reportado pelo OpenRouter (limite, chave inválida, modelo indisponível, etc.). */
class OpenRouterException(message: String) : Exception(message)
