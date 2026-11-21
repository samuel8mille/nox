package com.sam.nox.smoke

import com.sam.nox.mcp.McpManager

/**
 * Verificação rápida (fora da app Compose) de que o McpManager lê o ~/.nox/mcp.json,
 * expande as variáveis de ambiente (ex.: ${GITHUB_TOKEN}), arranca os servidores e
 * descobre as ferramentas. Uso: ./gradlew :desktopApp:runMcpSmoke
 */
fun main() {
    val manager = McpManager()
    val tools = manager.loadTools()
    val byServer = tools.groupBy { it.name.substringBefore("__") }
    println("=== Servidores MCP e nº de ferramentas ===")
    byServer.forEach { (server, ts) -> println("  $server: ${ts.size} ferramentas") }

    val github = tools.map { it.name.substringAfter("__") }.filter { it in GITHUB_ACTION_TOOLS }
    println("=== Ferramentas de Actions/workflows (github) ===")
    github.sorted().forEach { println("  - $it") }

    manager.close()
}

private val GITHUB_ACTION_TOOLS =
    setOf("actions_list", "actions_get", "actions_run_trigger", "get_job_logs")
