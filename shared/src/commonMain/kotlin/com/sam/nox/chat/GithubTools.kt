package com.sam.nox.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// --- Modelos das requisições/respostas da API REST do GitHub ---

@Serializable
private data class CreateRepoBody(
    val name: String,
    val description: String = "",
    val private: Boolean = true,
    val auto_init: Boolean = true,
)

@Serializable
private data class CreateIssueBody(
    val title: String,
    val body: String = "",
)

@Serializable
private data class CreatePrBody(
    val title: String,
    val head: String,
    val base: String,
    val body: String = "",
)

@Serializable
private data class RepoResult(
    val full_name: String = "",
    val html_url: String = "",
)

@Serializable
private data class IssueOrPrResult(
    val number: Int = 0,
    val html_url: String = "",
)

@Serializable
private data class UserResult(val login: String = "")

/**
 * Cliente mínimo da API REST do GitHub (api.github.com) com autenticação via token.
 * Cada método devolve texto legível (sucesso ou erro) para a assistente repassar.
 */
class GithubClient(
    private val token: String,
    private val httpClient: HttpClient = createHttpClient(),
    private val baseUrl: String = "https://api.github.com",
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        header("Authorization", "Bearer $token")
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("User-Agent", "Nox")
    }

    /** Descobre o login do usuário dono do token (para montar owner/repo). */
    private suspend fun currentUser(): String {
        val resp = httpClient.get("$baseUrl/user") { auth() }
        if (!resp.status.isSuccess()) error("não consegui identificar o usuário do token")
        return json.decodeFromString<UserResult>(resp.bodyAsText()).login
    }

    suspend fun createRepo(name: String, description: String, private: Boolean): String {
        val resp = httpClient.post("$baseUrl/user/repos") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateRepoBody(name, description, private)))
        }
        return handle(resp) {
            val r = json.decodeFromString<RepoResult>(it)
            "Repositório criado: ${r.full_name} — ${r.html_url}"
        }
    }

    suspend fun createIssue(repo: String, title: String, body: String): String {
        val owner = ownerFor(repo)
        val resp = httpClient.post("$baseUrl/repos/$owner/${repoName(repo)}/issues") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateIssueBody(title, body)))
        }
        return handle(resp) {
            val r = json.decodeFromString<IssueOrPrResult>(it)
            "Issue #${r.number} criada: ${r.html_url}"
        }
    }

    suspend fun createPullRequest(
        repo: String, title: String, head: String, base: String, body: String,
    ): String {
        val owner = ownerFor(repo)
        val resp = httpClient.post("$baseUrl/repos/$owner/${repoName(repo)}/pulls") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreatePrBody(title, head, base, body)))
        }
        return handle(resp) {
            val r = json.decodeFromString<IssueOrPrResult>(it)
            "Pull request #${r.number} aberto: ${r.html_url}"
        }
    }

    /** Resolve o owner: usa o informado em "owner/repo", senão o dono do token. */
    private suspend fun ownerFor(repo: String): String =
        if ("/" in repo) repo.substringBefore("/") else currentUser()

    private fun repoName(repo: String): String =
        if ("/" in repo) repo.substringAfter("/") else repo

    /** Converte a resposta HTTP em texto: sucesso via [onOk], ou erro legível. */
    private suspend fun handle(resp: HttpResponse, onOk: (String) -> String): String {
        val text = resp.bodyAsText()
        if (resp.status.isSuccess()) return onOk(text)
        val msg = runCatching {
            json.decodeFromString<JsonObject>(text)["message"]?.let { (it as? JsonPrimitive)?.content }
        }.getOrNull()
        return when (resp.status) {
            HttpStatusCode.Unauthorized -> "Erro: token do GitHub inválido ou expirado."
            HttpStatusCode.Forbidden -> "Erro: sem permissão (verifique os escopos do token). ${msg.orEmpty()}"
            HttpStatusCode.NotFound -> "Erro: repositório não encontrado. ${msg.orEmpty()}"
            HttpStatusCode.UnprocessableEntity -> "Erro: ${msg ?: "dados inválidos (ex.: repositório já existe)."}"
            else -> "Erro do GitHub (${resp.status.value}): ${msg ?: text.take(200)}"
        }
    }
}

// ----------------------------------------------------------------------------
// Ferramentas expostas ao modelo
// ----------------------------------------------------------------------------

private fun strProp(desc: String) = JsonObject(
    mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive(desc)),
)

private fun boolProp(desc: String) = JsonObject(
    mapOf("type" to JsonPrimitive("boolean"), "description" to JsonPrimitive(desc)),
)

private fun objectSchema(props: Map<String, JsonObject>, required: List<String>) = JsonObject(
    mapOf(
        "type" to JsonPrimitive("object"),
        "properties" to JsonObject(props),
        "required" to kotlinx.serialization.json.JsonArray(required.map { JsonPrimitive(it) }),
    ),
)

private fun arg(args: Map<String, kotlinx.serialization.json.JsonElement>, key: String): String =
    (args[key] as? JsonPrimitive)?.content?.trim().orEmpty()

private fun argBool(args: Map<String, kotlinx.serialization.json.JsonElement>, key: String, default: Boolean): Boolean =
    (args[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: default

/**
 * Cria o conjunto de ferramentas de GitHub (criar repositório, issue e pull request).
 * Todas exigem confirmação do usuário antes de executar.
 *
 * @param token GitHub Personal Access Token (escopo `repo`).
 */
fun githubTools(token: String, httpClient: HttpClient = createHttpClient()): List<Tool> {
    val client = GithubClient(token, httpClient)

    val criarRepo = Tool(
        name = "criar_repositorio_github",
        description = "Cria um novo repositório no GitHub da conta do usuário.",
        parameters = objectSchema(
            props = mapOf(
                "nome" to strProp("Nome do repositório (sem espaços)."),
                "descricao" to strProp("Descrição curta do repositório."),
                "privado" to boolProp("Se o repositório é privado (true) ou público (false)."),
            ),
            required = listOf("nome"),
        ),
        requiresConfirmation = true,
        preview = { a ->
            val vis = if (argBool(a, "privado", true)) "privado" else "público"
            "Criar repositório $vis \"${arg(a, "nome")}\" no seu GitHub."
        },
    ) { a ->
        client.createRepo(arg(a, "nome"), arg(a, "descricao"), argBool(a, "privado", true))
    }

    val criarIssue = Tool(
        name = "criar_issue_github",
        description = "Cria uma issue num repositório do GitHub.",
        parameters = objectSchema(
            props = mapOf(
                "repositorio" to strProp("Repositório no formato 'owner/repo' ou só 'repo'."),
                "titulo" to strProp("Título da issue."),
                "corpo" to strProp("Descrição/corpo da issue."),
            ),
            required = listOf("repositorio", "titulo"),
        ),
        requiresConfirmation = true,
        preview = { a -> "Criar issue \"${arg(a, "titulo")}\" em ${arg(a, "repositorio")}." },
    ) { a ->
        client.createIssue(arg(a, "repositorio"), arg(a, "titulo"), arg(a, "corpo"))
    }

    val abrirPr = Tool(
        name = "abrir_pull_request_github",
        description = "Abre um pull request num repositório do GitHub.",
        parameters = objectSchema(
            props = mapOf(
                "repositorio" to strProp("Repositório no formato 'owner/repo' ou só 'repo'."),
                "titulo" to strProp("Título do pull request."),
                "origem" to strProp("Branch de origem (head), com as mudanças."),
                "destino" to strProp("Branch de destino (base), ex.: main."),
                "corpo" to strProp("Descrição do pull request."),
            ),
            required = listOf("repositorio", "titulo", "origem", "destino"),
        ),
        requiresConfirmation = true,
        preview = { a ->
            "Abrir PR \"${arg(a, "titulo")}\" em ${arg(a, "repositorio")} " +
                "(${arg(a, "origem")} → ${arg(a, "destino")})."
        },
    ) { a ->
        client.createPullRequest(
            arg(a, "repositorio"), arg(a, "titulo"), arg(a, "origem"), arg(a, "destino"), arg(a, "corpo"),
        )
    }

    return listOf(criarRepo, criarIssue, abrirPr)
}
