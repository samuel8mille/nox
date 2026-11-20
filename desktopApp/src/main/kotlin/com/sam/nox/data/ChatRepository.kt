package com.sam.nox.data

import com.sam.nox.chat.OpenRouterClient
import com.sam.nox.chat.Tool
import com.sam.nox.chat.builtInTools
import com.sam.nox.chat.githubTools
import com.sam.nox.chat.webSearchTool
import com.sam.nox.systemTools

/**
 * Camada de dados do chat. Usa o Groq (endpoint compatível com OpenAI, rápido e
 * com limite por-minuto) como LLM, via [OpenRouterClient] apontado ao baseUrl do Groq.
 *
 * Chaves (variáveis de ambiente / -D):
 * - GROQ_API_KEY: obrigatória (chat).
 * - TAVILY_API_KEY: opcional (busca web).
 * - GITHUB_TOKEN: opcional (criar repositório/issue/PR).
 *
 * @param confirm pedido de confirmação para ações que alteram algo externo. Recebe
 *                o preview da ação e devolve true (aprovar) / false (recusar).
 */
class ChatRepository(
    val modelName: String = "openai/gpt-oss-120b",
    apiKey: String = resolveApiKey(),
    tavilyKey: String? = resolveTavilyKey(),
    githubToken: String? = resolveGithubToken(),
    confirm: suspend (preview: String) -> Boolean = { true },
    private val client: OpenRouterClient = OpenRouterClient(
        apiKey = apiKey,
        model = modelName,
        tools = buildTools(tavilyKey, githubToken),
        fallbackModels = listOf("openai/gpt-oss-20b"),
        confirm = confirm,
        baseUrl = "https://api.groq.com/openai/v1",
    ),
) {
    /** Envia a mensagem do usuário e devolve a resposta da assistente. */
    suspend fun send(userMessage: String): String = client.send(userMessage)

    /** Limpa o histórico, começando uma conversa nova. */
    fun reset() = client.reset()

    fun close() = client.close()

    companion object {
        /** Lê a API key do Groq de -Dgroq.key, depois de GROQ_API_KEY. */
        fun resolveApiKey(): String =
            System.getProperty("groq.key")
                ?: System.getenv("GROQ_API_KEY")
                ?: error(
                    "Defina a API key do Groq em GROQ_API_KEY (ou -Dgroq.key). " +
                        "Crie a sua em https://console.groq.com/keys",
                )

        /** Lê a chave opcional do Tavily (busca web). Null = busca desligada. */
        fun resolveTavilyKey(): String? =
            System.getProperty("tavily.key") ?: System.getenv("TAVILY_API_KEY")

        /** Lê o token opcional do GitHub. Null = ferramentas de GitHub desligadas. */
        fun resolveGithubToken(): String? =
            System.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN")

        /**
         * Ferramentas: internas + sistema (terminal/arquivos) + busca web (se Tavily)
         * + GitHub (se token). As de sistema exigem confirmação do usuário.
         */
        private fun buildTools(tavilyKey: String?, githubToken: String?): List<Tool> =
            buildList {
                addAll(builtInTools)
                addAll(systemTools())
                if (!tavilyKey.isNullOrBlank()) add(webSearchTool(apiKey = tavilyKey))
                if (!githubToken.isNullOrBlank()) addAll(githubTools(token = githubToken))
            }
    }
}
