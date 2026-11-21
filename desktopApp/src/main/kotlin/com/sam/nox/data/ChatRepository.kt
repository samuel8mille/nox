package com.sam.nox.data

import com.sam.nox.chat.ConfirmDecision
import com.sam.nox.chat.OpenRouterClient
import com.sam.nox.chat.Tool
import com.sam.nox.chat.builtInTools
import com.sam.nox.chat.githubTools
import com.sam.nox.chat.webSearchTool
import com.sam.nox.mcp.McpManager
import com.sam.nox.memory.MemoryStore
import com.sam.nox.memory.memoryTools
import com.sam.nox.tools.systemTools

/**
 * Camada de dados do chat. Usa o Groq (endpoint compatível com OpenAI, rápido e
 * com limite por-minuto) como LLM, via [OpenRouterClient] apontado ao baseUrl do Groq.
 *
 * Chaves (variáveis de ambiente / -D):
 * - GROQ_API_KEY: obrigatória (chat).
 * - TAVILY_API_KEY: opcional (busca web).
 * - GITHUB_TOKEN: opcional (criar repositório/issue/PR).
 *
 * @param confirm Pedido de confirmação para ações que alteram algo externo. Recebe o nome da
 *   ferramenta e o preview da ação, e devolve a decisão (recusar / aprovar uma vez / aprovar sempre
 *   nesta conversa).
 * @param memory Armazenamento dos fatos lembrados sobre o usuário (injetado, não instanciado aqui).
 * @param mcp Gestor dos servidores MCP configurados (injetado; o [close] desta classe o encerra).
 */
class ChatRepository(
    val modelName: String = "qwen/qwen3.8-27b",
    apiKey: String = resolveApiKey(),
    tavilyKey: String? = resolveTavilyKey(),
    githubToken: String? = resolveGithubToken(),
    confirm: suspend (toolName: String, preview: String) -> ConfirmDecision =
        { _, _ -> ConfirmDecision.APPROVE_ONCE },
    private val memory: MemoryStore,
    private val mcp: McpManager,
) {
    private val client: OpenRouterClient =
        OpenRouterClient(
            apiKey = apiKey,
            model = modelName,
            tools = buildTools(tavilyKey, githubToken, memory, mcp),
            fallbackModels = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b"),
            confirm = confirm,
            extraContext = { memoryContext(memory) },
            baseUrl = "https://api.groq.com/openai/v1",
        )

    /** Envia a mensagem do usuário e devolve a resposta da assistente. */
    suspend fun send(userMessage: String): String = client.send(userMessage)

    /** Limpa o histórico, começando uma conversa nova (a memória persiste). */
    fun reset() = client.reset()

    fun close() {
        client.close()
        mcp.close()
    }

    companion object {
        /** Lê a API key do Groq de -Dgroq.key, depois de GROQ_API_KEY. */
        fun resolveApiKey(): String = System.getProperty("groq.key")
            ?: System.getenv("GROQ_API_KEY")
            ?: error(
                "Defina a API key do Groq em GROQ_API_KEY (ou -Dgroq.key). " +
                    "Crie a sua em https://console.groq.com/keys",
            )

        /** Lê a chave opcional do Tavily (busca web). Null = busca desligada. */
        fun resolveTavilyKey(): String? = System.getProperty("tavily.key") ?: System.getenv("TAVILY_API_KEY")

        /** Lê o token opcional do GitHub. Null = ferramentas de GitHub desligadas. */
        fun resolveGithubToken(): String? = System.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN")

        /**
         * Ferramentas: internas + memória + sistema (terminal/arquivos) + busca web
         * (se Tavily) + GitHub (se token) + servidores MCP configurados. Sistema, GitHub
         * e MCP exigem confirmação.
         */
        private fun buildTools(
            tavilyKey: String?,
            githubToken: String?,
            memory: MemoryStore,
            mcp: McpManager,
        ): List<Tool> = buildList {
            addAll(builtInTools)
            addAll(memoryTools(memory))
            addAll(systemTools())
            if (!tavilyKey.isNullOrBlank()) add(webSearchTool(apiKey = tavilyKey))
            if (!githubToken.isNullOrBlank()) addAll(githubTools(token = githubToken))
            addAll(mcp.loadTools())
        }

        /** Monta o bloco de contexto com os fatos lembrados, injetado no system prompt. */
        private fun memoryContext(memory: MemoryStore): String {
            val fatos = memory.list()
            if (fatos.isEmpty()) return ""
            return buildString {
                appendLine("O que você já sabe sobre o usuário (use quando for relevante):")
                fatos.forEach { appendLine("- ${it.text}") }
            }.trim()
        }
    }
}
