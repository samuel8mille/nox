package com.sam.nox.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolSelectorTest {
    private fun tool(name: String) = Tool(
        name = name,
        description = "ferramenta $name",
        parameters = noParameters,
        execute = { "" },
    )

    /** Núcleo (sem prefixo) + grupos MCP (prefixo grupo__). */
    private fun sampleTools(): List<Tool> = buildList {
        // núcleo
        add(tool("obter_hora_atual"))
        add(tool("calcular"))
        add(tool("pesquisar_web"))
        // github (prefixo)
        repeat(45) { add(tool("github__action_$it")) }
        // google (prefixo)
        repeat(60) { add(tool("google__gmail_$it")) }
        // fs / browser (prefixo)
        repeat(16) { add(tool("fs__op_$it")) }
        repeat(25) { add(tool("browser__op_$it")) }
    }

    private fun names(tools: List<Tool>) = tools.map { it.name }.toSet()

    @Test
    fun coreToolsAlwaysIncluded() {
        val selected = ToolSelector.select(sampleTools(), recentText = "oi, tudo bem?")
        assertTrue("obter_hora_atual" in names(selected))
        assertTrue("calcular" in names(selected))
        assertTrue("pesquisar_web" in names(selected))
    }

    @Test
    fun noGroupKeywordExcludesAllMcpGroups() {
        val selected = ToolSelector.select(sampleTools(), recentText = "me conte uma piada")
        assertFalse(names(selected).any { it.startsWith("github__") })
        assertFalse(names(selected).any { it.startsWith("google__") })
        // só o núcleo
        assertEquals(3, selected.size)
    }

    @Test
    fun emailKeywordActivatesGoogleGroup() {
        val selected = ToolSelector.select(sampleTools(), recentText = "lê o meu último email")
        assertTrue(names(selected).any { it.startsWith("google__") })
        assertFalse(names(selected).any { it.startsWith("github__") })
    }

    @Test
    fun repoKeywordActivatesGithubGroup() {
        val selected = ToolSelector.select(sampleTools(), recentText = "lista os workflows do repositório")
        assertTrue(names(selected).any { it.startsWith("github__") })
        assertFalse(names(selected).any { it.startsWith("google__") })
    }

    @Test
    fun accentInsensitiveMatching() {
        // "calendário" com acento deve ativar o grupo google.
        val selected = ToolSelector.select(sampleTools(), recentText = "marca no meu calendário")
        assertTrue(names(selected).any { it.startsWith("google__") })
    }

    @Test
    fun capsToolsPerGroup() {
        // Grupo google tem 60 tools; com teto de 25 por grupo, só 25 devem entrar.
        val selected =
            ToolSelector.select(
                sampleTools(),
                recentText = "lê o meu email no gmail",
                maxToolsPerGroup = 25,
            )
        val googleCount = selected.count { it.name.startsWith("google__") }
        assertTrue(googleCount <= 25, "esperava <=25 tools google, veio $googleCount")
    }

    @Test
    fun prefersRelevantToolsWithinGroup() {
        // Com um nome específico, a tool correspondente deve sobreviver ao corte do grupo.
        val tools =
            buildList {
                add(tool("obter_hora_atual"))
                repeat(40) { add(tool("google__gmail_misc_$it")) }
                add(tool("google__calendar_create_event")) // alvo
            }
        val selected =
            ToolSelector.select(
                tools,
                recentText = "cria um evento no calendar",
                maxToolsPerGroup = 5,
            )
        assertTrue("google__calendar_create_event" in names(selected))
    }

    @Test
    fun respectsMaxToolsCeiling() {
        // Ativa github E google (muitas tools) e força um teto baixo.
        val selected =
            ToolSelector.select(
                sampleTools(),
                recentText = "veja meu email e os repositórios do github",
                maxTools = 50,
            )
        assertTrue(selected.size <= 50)
        // o núcleo tem de sobreviver ao corte
        assertTrue("obter_hora_atual" in names(selected))
    }
}
