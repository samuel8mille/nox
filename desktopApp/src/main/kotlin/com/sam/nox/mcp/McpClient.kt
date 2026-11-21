package com.sam.nox.mcp

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/** Uma ferramenta exposta por um servidor MCP (resultado de `tools/list`). */
data class McpTool(val name: String, val description: String, val inputSchema: JsonObject)

/**
 * Cliente MCP por transporte stdio: lança o servidor como processo e troca mensagens
 * JSON-RPC 2.0 (uma por linha) por stdin/stdout. Implementa o mínimo necessário:
 * `initialize`, `tools/list` e `tools/call`.
 *
 * As respostas são lidas numa thread dedicada e casadas por `id` com as requisições.
 */
class McpClient(
    private val command: List<String>,
    /** Variáveis de ambiente extra para o processo do servidor (ex.: tokens). */
    private val env: Map<String, String> = emptyMap(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var readerThread: Thread? = null

    private var nextId = 1
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()

    /** Inicia o processo, lê respostas em background e completa o handshake `initialize`. */
    suspend fun start() = withContext(Dispatchers.IO) {
        val builder =
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
        // Herda o ambiente do Nox e acrescenta as variáveis configuradas.
        if (env.isNotEmpty()) builder.environment().putAll(env)
        val proc = builder.start()
        process = proc
        writer = proc.outputStream.bufferedWriter()
        reader = proc.inputStream.bufferedReader()

        readerThread =
            thread(name = "mcp-reader", isDaemon = true) {
                try {
                    reader?.forEachLine { line ->
                        if (line.isBlank()) return@forEachLine
                        val obj =
                            runCatching { json.parseToJsonElement(line).jsonObject }.getOrNull()
                                ?: return@forEachLine
                        val id = obj["id"]?.jsonPrimitive?.intOrNull()
                        if (id != null) pending.remove(id)?.complete(obj)
                    }
                } catch (_: Throwable) {
                    // stream encerrado; requisições pendentes falham no timeout
                }
            }

        // Handshake MCP.
        request(
            "initialize",
            buildJsonObject {
                put("protocolVersion", "2024-11-05")
                putJsonObject("capabilities") {}
                putJsonObject("clientInfo") {
                    put("name", "Nox")
                    put("version", "1.0")
                }
            },
        )
        notify("notifications/initialized", buildJsonObject {})
    }

    /** Lista as ferramentas expostas pelo servidor. */
    suspend fun listTools(): List<McpTool> {
        val result = request("tools/list", buildJsonObject {})["result"]?.jsonObject ?: return emptyList()
        val tools = result["tools"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return tools.mapNotNull { el ->
            val o = el.jsonObject
            val name = o["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            McpTool(
                name = name,
                description = o["description"]?.jsonPrimitive?.content.orEmpty(),
                inputSchema = o["inputSchema"]?.jsonObject ?: JsonObject(emptyMap()),
            )
        }
    }

    /** Executa uma ferramenta e devolve o resultado como texto. */
    suspend fun callTool(name: String, arguments: Map<String, JsonElement>): String {
        val resp =
            request(
                "tools/call",
                buildJsonObject {
                    put("name", name)
                    put("arguments", JsonObject(arguments))
                },
            )
        resp["error"]?.jsonObject?.let { err ->
            return "Erro MCP: ${err["message"]?.jsonPrimitive?.content ?: "falha na ferramenta."}"
        }
        val result = resp["result"]?.jsonObject ?: return "(sem resultado)"
        // content é uma lista de blocos; extraímos o texto.
        val content = result["content"] as? kotlinx.serialization.json.JsonArray
        return content
            ?.mapNotNull { (it.jsonObject["text"] as? JsonPrimitive)?.content }
            ?.joinToString("\n")
            ?.ifBlank { "(sem texto)" }
            ?: result.toString().take(2000)
    }

    fun close() {
        runCatching { writer?.close() }
        runCatching { process?.destroy() }
        process = null
    }

    // --- JSON-RPC ---

    private suspend fun request(method: String, params: JsonObject): JsonObject {
        val id = nextId++
        val deferred = CompletableDeferred<JsonObject>()
        pending[id] = deferred
        send(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", id)
                put("method", method)
                put("params", params)
            },
        )
        return try {
            withTimeout(30_000) { deferred.await() }
        } catch (e: Throwable) {
            pending.remove(id)
            throw RuntimeException("MCP '$method' sem resposta: ${e.message}")
        }
    }

    private fun notify(method: String, params: JsonObject) {
        send(
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", method)
                put("params", params)
            },
        )
    }

    private fun send(message: JsonObject) {
        val w = writer ?: return
        synchronized(w) {
            w.write(json.encodeToString(JsonObject.serializer(), message))
            w.write("\n")
            w.flush()
        }
    }
}

/** intOrNull seguro para JsonPrimitive. */
private fun JsonPrimitive.intOrNull(): Int? = content.toIntOrNull()
