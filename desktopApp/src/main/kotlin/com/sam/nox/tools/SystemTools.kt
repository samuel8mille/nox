package com.sam.nox

import com.sam.nox.chat.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.util.concurrent.TimeUnit

// --- Helpers de schema ---

private fun strProp(desc: String) = JsonObject(
    mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive(desc)),
)

private fun schema(props: Map<String, JsonObject>, required: List<String>) = JsonObject(
    mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(props),
        "required" to kotlinx.serialization.json.JsonArray(required.map { JsonPrimitive(it) }),
    ),
)

private fun arg(args: Map<String, JsonElement>, key: String): String =
    (args[key] as? JsonPrimitive)?.content?.trim().orEmpty()

/** Resolve um caminho relativo contra a pasta de trabalho atual (se houver). */
private fun resolvePath(workingDir: File?, path: String): File {
    val f = File(path)
    return if (f.isAbsolute || workingDir == null) f else File(workingDir, path)
}

/**
 * Ferramentas de sistema para o Desktop: executar comandos no terminal, criar/ler
 * arquivos e listar pastas. A pasta de trabalho é definida pelo usuário na conversa
 * (ferramenta `definir_pasta_de_trabalho`) e os caminhos relativos resolvem a partir dela.
 *
 * O terminal exige confirmação do usuário a cada comando (requiresConfirmation).
 */
fun systemTools(): List<Tool> {
    // Pasta de trabalho corrente, compartilhada entre as ferramentas desta sessão.
    var workingDir: File? = null

    val definirPasta = Tool(
        name = "definir_pasta_de_trabalho",
        description = "Define a pasta (diretório) onde as próximas ações de arquivo e " +
            "terminal vão operar. Use quando o usuário indicar em qual projeto/pasta trabalhar.",
        parameters = schema(
            props = mapOf("caminho" to strProp("Caminho absoluto da pasta, ex.: /Users/você/projeto.")),
            required = listOf("caminho"),
        ),
    ) { a ->
        val dir = File(arg(a, "caminho"))
        when {
            !dir.exists() -> "Erro: a pasta '${dir.path}' não existe."
            !dir.isDirectory -> "Erro: '${dir.path}' não é uma pasta."
            else -> { workingDir = dir; "Pasta de trabalho definida: ${dir.path}" }
        }
    }

    val executarTerminal = Tool(
        name = "executar_terminal",
        description = "Executa um comando no terminal (shell) dentro da pasta de trabalho. " +
            "Use para git (status, add, commit, push), rodar scripts, etc. Devolve a saída.",
        parameters = schema(
            props = mapOf("comando" to strProp("O comando de shell a executar, ex.: 'git status'.")),
            required = listOf("comando"),
        ),
        requiresConfirmation = true,
        preview = { a ->
            val onde = workingDir?.path ?: "pasta atual"
            "Executar no terminal (em $onde):\n${arg(a, "comando")}"
        },
    ) { a ->
        val comando = arg(a, "comando")
        if (comando.isBlank()) return@Tool "Erro: comando vazio."
        withContext(Dispatchers.IO) {
            try {
                val proc = ProcessBuilder("/bin/zsh", "-lc", comando)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start()
                val finished = proc.waitFor(120, TimeUnit.SECONDS)
                if (!finished) {
                    proc.destroyForcibly()
                    return@withContext "Erro: o comando demorou demais (timeout 120s)."
                }
                val saida = proc.inputStream.bufferedReader().readText().trim()
                val code = proc.exitValue()
                buildString {
                    appendLine("Código de saída: $code")
                    if (saida.isNotEmpty()) {
                        appendLine("Saída:")
                        append(saida.take(4000))
                    } else appendLine("(sem saída)")
                }.trim()
            } catch (e: Throwable) {
                "Erro ao executar: ${e.message}"
            }
        }
    }

    val criarArquivo = Tool(
        name = "criar_arquivo",
        description = "Cria ou sobrescreve um arquivo com o conteúdo informado, na pasta de trabalho.",
        parameters = schema(
            props = mapOf(
                "caminho" to strProp("Caminho do arquivo (relativo à pasta de trabalho ou absoluto)."),
                "conteudo" to strProp("Conteúdo de texto do arquivo."),
            ),
            required = listOf("caminho", "conteudo"),
        ),
        requiresConfirmation = true,
        preview = { a -> "Criar/sobrescrever o arquivo: ${arg(a, "caminho")}" },
    ) { a ->
        withContext(Dispatchers.IO) {
            try {
                val file = resolvePath(workingDir, arg(a, "caminho"))
                file.parentFile?.mkdirs()
                file.writeText(arg(a, "conteudo"))
                "Arquivo criado: ${file.path} (${file.length()} bytes)"
            } catch (e: Throwable) {
                "Erro ao criar arquivo: ${e.message}"
            }
        }
    }

    val lerArquivo = Tool(
        name = "ler_arquivo",
        description = "Lê o conteúdo de um arquivo de texto na pasta de trabalho.",
        parameters = schema(
            props = mapOf("caminho" to strProp("Caminho do arquivo (relativo ou absoluto).")),
            required = listOf("caminho"),
        ),
    ) { a ->
        withContext(Dispatchers.IO) {
            try {
                val file = resolvePath(workingDir, arg(a, "caminho"))
                if (!file.exists()) "Erro: arquivo '${file.path}' não existe."
                else file.readText().take(6000)
            } catch (e: Throwable) {
                "Erro ao ler arquivo: ${e.message}"
            }
        }
    }

    val listarPasta = Tool(
        name = "listar_pasta",
        description = "Lista os arquivos e subpastas da pasta de trabalho (ou de um caminho).",
        parameters = schema(
            props = mapOf("caminho" to strProp("Caminho da pasta (opcional; vazio = pasta de trabalho).")),
            required = emptyList(),
        ),
    ) { a ->
        withContext(Dispatchers.IO) {
            try {
                val dir = if (arg(a, "caminho").isNotEmpty()) resolvePath(workingDir, arg(a, "caminho"))
                else workingDir ?: return@withContext "Erro: defina a pasta de trabalho primeiro."
                if (!dir.isDirectory) return@withContext "Erro: '${dir.path}' não é uma pasta."
                val itens = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                buildString {
                    appendLine("Conteúdo de ${dir.path}:")
                    itens.forEach { f -> appendLine(if (f.isDirectory) "📁 ${f.name}/" else "📄 ${f.name}") }
                }.trim()
            } catch (e: Throwable) {
                "Erro ao listar: ${e.message}"
            }
        }
    }

    return listOf(definirPasta, executarTerminal, criarArquivo, lerArquivo, listarPasta)
}
