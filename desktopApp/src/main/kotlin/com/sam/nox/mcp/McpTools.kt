package com.sam.nox.mcp

import com.sam.nox.chat.Tool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

// --- Configuração (~/.nox/mcp.json) ---

@Serializable
private data class McpServerConfig(
    val command: String,
    val args: List<String> = emptyList(),
    /**
     * Variáveis de ambiente para o processo do servidor. Os valores suportam
     * `${VAR}`, que é expandido a partir do ambiente do Nox — assim um token pode
     * ficar como `"${GITHUB_TOKEN}"` sem o escrever em claro no mcp.json.
     */
    val env: Map<String, String> = emptyMap(),
)

@Serializable
private data class McpConfig(val mcpServers: Map<String, McpServerConfig> = emptyMap())

/**
 * Gerencia os servidores MCP configurados: inicia-os, descobre suas ferramentas e as
 * adapta para a interface [Tool] do Nox. Cada ferramenta MCP exige confirmação.
 *
 * Use [loadTools] para obter as ferramentas e [close] para encerrar os processos.
 */
class McpManager(private val configFile: File = File(System.getProperty("user.home"), ".nox/mcp.json")) {
    private val json = Json { ignoreUnknownKeys = true }
    private val clients = mutableListOf<McpClient>()

    /** Lê a config, inicia os servidores e devolve todas as ferramentas descobertas. */
    fun loadTools(): List<Tool> {
        val config = readConfig() ?: return emptyList()
        return config.mcpServers.flatMap { (serverName, cfg) ->
            runCatching { startServer(serverName, cfg) }
                .getOrElse {
                    println("[MCP] falha ao iniciar '$serverName': ${it.message}")
                    emptyList()
                }
        }
    }

    private fun startServer(serverName: String, cfg: McpServerConfig): List<Tool> = runBlocking {
        val client = McpClient(listOf(cfg.command) + cfg.args, env = expandEnv(cfg.env))
        client.start()
        clients.add(client)
        val mcpTools = client.listTools()
        mcpTools.map { it.toNoxTool(serverName, client) }
    }

    /**
     * Expande `${VAR}` em cada valor a partir do ambiente (ou system properties).
     * Variáveis inexistentes expandem para vazio. Entradas que resultem em valor
     * vazio são descartadas, para não passar um token em branco ao servidor.
     */
    private fun expandEnv(env: Map<String, String>): Map<String, String> = env
        .mapValues { (_, value) ->
            Regex("""\$\{([^}]+)}""").replace(value) { m ->
                val name = m.groupValues[1]
                System.getenv(name) ?: System.getProperty(name) ?: ""
            }
        }.filterValues { it.isNotBlank() }

    private fun readConfig(): McpConfig? = try {
        if (configFile.exists()) {
            json.decodeFromString(McpConfig.serializer(), configFile.readText())
        } else {
            null
        }
    } catch (e: Throwable) {
        println("[MCP] erro ao ler ${configFile.path}: ${e.message}")
        null
    }

    /** Encerra todos os servidores MCP iniciados por este gestor. */
    fun close() {
        clients.forEach { runCatching { it.close() } }
        clients.clear()
    }
}

/** Adapta uma ferramenta MCP para a interface Tool do Nox, exigindo confirmação. */
private fun McpTool.toNoxTool(serverName: String, client: McpClient): Tool = Tool(
    // Prefixo pelo servidor evita colisão de nomes entre servidores.
    name = "${serverName}__$name".take(64),
    description = description.ifBlank { "Ferramenta '$name' do servidor MCP '$serverName'." },
    parameters = inputSchema.ifEmptyObject(),
    requiresConfirmation = true,
    preview = { args ->
        val argsText =
            args.entries.joinToString(", ") { (k, v) ->
                "$k=${(v as? JsonPrimitive)?.content ?: v}"
            }
        "Executar ferramenta MCP '$name' ($serverName)" + if (argsText.isNotBlank()) ": $argsText" else "."
    },
    execute = { args: Map<String, JsonElement> -> client.callTool(name, args) },
)

/** Garante um JSON Schema de objeto válido (alguns servidores omitem/entregam vazio). */
private fun JsonObject.ifEmptyObject(): JsonObject = if (this.isEmpty()) {
    JsonObject(
        mapOf("type" to JsonPrimitive("object"), "properties" to JsonObject(emptyMap())),
    )
} else {
    this
}
