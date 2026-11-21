package com.sam.nox

import com.sam.nox.data.ChatRepository
import kotlinx.coroutines.runBlocking

/**
 * Teste ponta-a-ponta do fluxo real do chat com uma ação de GitHub via MCP:
 * usa o mesmo ChatRepository.send() que a UI usa. Auto-aprova as confirmações
 * (imprimindo o preview) e envia um comando que leva o modelo a usar as
 * ferramentas de GitHub. Uso: ./gradlew :desktopApp:runGithubChatSmoke
 */
fun main() = runBlocking {
    val repo = ChatRepository(
        confirm = { preview ->
            println("\n[CONFIRMAÇÃO AUTO-APROVADA] $preview\n")
            true
        },
    )
    try {
        val pergunta = "Liste os 5 repositórios mais recentes da minha conta no GitHub. " +
            "Use as ferramentas de GitHub disponíveis."
        println(">>> Usuário: $pergunta\n")
        val resposta = repo.send(pergunta)
        println("\n=== Resposta do Nox ===\n$resposta")
    } finally {
        repo.close()
    }
}
