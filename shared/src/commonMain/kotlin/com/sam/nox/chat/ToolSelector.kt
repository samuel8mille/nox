package com.sam.nox.chat

/**
 * Seleção dinâmica de ferramentas.
 *
 * Provedores compatíveis com OpenAI (Groq incluído) limitam o número de ferramentas
 * por requisição (Groq: 128). Com vários servidores MCP ligados (GitHub, Google, ...)
 * o total ultrapassa esse teto. Em vez de truncar cegamente, este seletor envia ao
 * modelo apenas as ferramentas relevantes à conversa atual:
 *
 * - Ferramentas "núcleo" (sem prefixo de servidor MCP — hora, cálculo, memória, busca,
 *   sistema, GitHub nativo) entram sempre: são poucas e quase sempre úteis.
 * - Ferramentas de um grupo MCP (prefixo `grupo__nome`) só entram quando a mensagem
 *   recente menciona palavras-chave desse grupo.
 * - No fim, respeita um teto [maxTools] para nunca exceder o limite do provedor.
 *
 * A seleção afeta só QUAIS ferramentas são oferecidas ao modelo; qualquer ferramenta
 * chamada continua a ser executável (o cliente mantém o mapa completo por nome).
 */
object ToolSelector {
    /** Teto padrão de ferramentas por requisição (abaixo do limite de 128 do Groq). */
    const val DEFAULT_MAX_TOOLS = 120

    /**
     * Teto de ferramentas por grupo MCP ativado. Mesmo cabendo no limite do provedor,
     * dezenas de ferramentas de um só grupo confundem o modelo (ele alucina ou não as
     * usa). Enviamos só as mais relevantes à mensagem, por grupo.
     */
    const val DEFAULT_MAX_TOOLS_PER_GROUP = 25

    /**
     * Palavras-chave (minúsculas, sem acento) que ativam cada grupo MCP, por prefixo.
     * O prefixo corresponde ao nome do servidor em `mcp.json` (ver McpTools: `serverName__tool`).
     */
    private val GROUP_KEYWORDS: Map<String, List<String>> =
        mapOf(
            "github" to
                listOf(
                    "github",
                    "git",
                    "repo",
                    "repositorio",
                    "repositório",
                    "pull request",
                    "pr ",
                    "issue",
                    "commit",
                    "branch",
                    "workflow",
                    "action",
                    "ci",
                    "release",
                    "tag",
                    "merge",
                    "fork",
                    "deploy",
                ),
            "google" to
                listOf(
                    "google",
                    "gmail",
                    "email",
                    "e-mail",
                    "mail",
                    "correio",
                    "calendar",
                    "calendario",
                    "calendário",
                    "agenda",
                    "evento",
                    "reuniao",
                    "reunião",
                    "drive",
                    "documento",
                    "docs",
                    "planilha",
                    "sheets",
                    "apresentacao",
                    "slides",
                    "marcar",
                    "agendar",
                    "compromisso",
                ),
            "fs" to
                listOf(
                    "arquivo",
                    "ficheiro",
                    "pasta",
                    "diretorio",
                    "diretório",
                    "file",
                    "folder",
                    "ler arquivo",
                    "escrever",
                    "salvar em",
                    "listar pasta",
                ),
            "browser" to
                listOf(
                    "navegar",
                    "navegador",
                    "browser",
                    "site",
                    "página",
                    "pagina",
                    "url",
                    "abrir no navegador",
                    "clicar",
                    "playwright",
                    "screenshot",
                    "web",
                ),
        )

    /** Devolve o nome do grupo MCP de uma ferramenta, ou null se for do núcleo. */
    private fun groupOf(toolName: String): String? {
        val idx = toolName.indexOf("__")
        return if (idx > 0) toolName.substring(0, idx) else null
    }

    /**
     * Seleciona as ferramentas a oferecer ao modelo para [recentText] (a mensagem do
     * usuário e, idealmente, um pouco do contexto recente).
     */
    fun select(
        all: List<Tool>,
        recentText: String,
        maxTools: Int = DEFAULT_MAX_TOOLS,
        maxToolsPerGroup: Int = DEFAULT_MAX_TOOLS_PER_GROUP,
    ): List<Tool> {
        val text = normalize(recentText)
        val words = text.split(Regex("""\W+""")).filter { it.length >= 3 }.toSet()

        val activeGroups =
            GROUP_KEYWORDS
                .filterValues { keywords -> keywords.any { text.contains(normalize(it)) } }
                .keys

        val core = all.filter { groupOf(it.name) == null }

        // Para cada grupo ativo, inclui só as ferramentas mais relevantes à mensagem
        // (pontuadas por sobreposição de palavras com nome+descrição), até ao teto do grupo.
        val fromActiveGroups =
            activeGroups.flatMap { group ->
                all
                    .filter { groupOf(it.name) == group }
                    .sortedByDescending { relevance(it, words) }
                    .take(maxToolsPerGroup)
            }

        val selected = core + fromActiveGroups

        // Garante o teto global do provedor, preservando sempre o núcleo.
        if (selected.size <= maxTools) return selected
        return (core + fromActiveGroups).take(maxTools)
    }

    /** Pontua a relevância de uma ferramenta às palavras da mensagem (nome + descrição). */
    private fun relevance(tool: Tool, words: Set<String>): Int {
        if (words.isEmpty()) return 0
        val haystack = normalize(tool.name.replace("_", " ") + " " + tool.description)
        return words.count { haystack.contains(it) }
    }

    /** minúsculas + remove acentos comuns do português, para casar palavras-chave. */
    private fun normalize(s: String): String {
        val lower = s.lowercase()
        val sb = StringBuilder(lower.length)
        for (c in lower) {
            sb.append(
                when (c) {
                    'á', 'à', 'â', 'ã', 'ä' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'í', 'ì', 'î', 'ï' -> 'i'
                    'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
                    'ú', 'ù', 'û', 'ü' -> 'u'
                    'ç' -> 'c'
                    else -> c
                },
            )
        }
        return sb.toString()
    }
}
