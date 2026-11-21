package com.sam.nox.smoke

import com.sam.nox.chat.ConfirmDecision
import com.sam.nox.data.ChatRepository
import com.sam.nox.mcp.McpManager
import com.sam.nox.memory.MemoryStore
import kotlinx.coroutines.runBlocking

/**
 * Teste ponta-a-ponta do fluxo real do chat com ferramentas MCP: usa o mesmo
 * ChatRepository.send() que a UI usa. Auto-aprova as confirmações (imprimindo o
 * preview) e envia um comando que leva o modelo a usar ferramentas.
 *
 * A pergunta pode vir por argumento; sem argumento, usa um comando de GitHub.
 * Uso: ./gradlew :desktopApp:runGithubChatSmoke --args="lê o meu último email"
 */
fun main(args: Array<String>) = runBlocking {
    val pergunta =
        args.joinToString(" ").ifBlank {
            "Liste os 5 repositórios mais recentes da minha conta no GitHub. " +
                "Use as ferramentas de GitHub disponíveis."
        }
    val repo =
        ChatRepository(
            confirm = { _, preview ->
                println("\n[CONFIRMAÇÃO AUTO-APROVADA] $preview\n")
                ConfirmDecision.APPROVE_ONCE
            },
            memory = MemoryStore(),
            mcp = McpManager(),
        )
    try {
        println(">>> Usuário: $pergunta\n")
        val resposta = repo.send(pergunta)
        println("\n=== Resposta do Nox ===\n$resposta")
    } finally {
        repo.close()
    }
}
