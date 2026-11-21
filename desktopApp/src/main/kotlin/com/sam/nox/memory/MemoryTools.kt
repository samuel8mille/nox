package com.sam.nox.memory

import com.sam.nox.chat.Tool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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

/**
 * Ferramentas de memória de longo prazo: lembrar fatos sobre o usuário, listá-los e
 * esquecê-los. Nenhuma exige confirmação (operam apenas na memória local do app).
 */
fun memoryTools(store: MemoryStore): List<Tool> {
    val lembrar =
        Tool(
            name = "lembrar",
            description =
            "Salva um fato de longo prazo sobre o usuário (nome, preferências, " +
                "projetos, contexto pessoal). Use quando o usuário revelar algo sobre si que seja " +
                "útil lembrar em conversas futuras, ou quando ele pedir explicitamente para lembrar.",
            parameters =
            schema(
                props = mapOf("fato" to strProp("O fato a lembrar, em uma frase curta e objetiva.")),
                required = listOf("fato"),
            ),
        ) { a ->
            val texto = arg(a, "fato")
            if (texto.isEmpty()) {
                "Erro: nada para lembrar."
            } else {
                store.add(texto)
                "Lembrado: $texto"
            }
        }

    val listar =
        Tool(
            name = "listar_memorias",
            description =
            "Lista tudo que você já sabe sobre o usuário. Use quando ele perguntar " +
                "o que você sabe/lembra sobre ele.",
            parameters = schema(props = emptyMap(), required = emptyList()),
        ) { _ ->
            val fatos = store.list()
            if (fatos.isEmpty()) {
                "Ainda não sei nada específico sobre o usuário."
            } else {
                buildString {
                    appendLine("O que sei sobre o usuário:")
                    fatos.forEach { appendLine("- ${it.text}") }
                }.trim()
            }
        }

    val esquecer =
        Tool(
            name = "esquecer",
            description = "Remove um fato da memória. Use quando o usuário pedir para esquecer algo.",
            parameters =
            schema(
                props = mapOf("fato" to strProp("Trecho que identifica o fato a remover.")),
                required = listOf("fato"),
            ),
        ) { a ->
            val removido = store.remove(arg(a, "fato"))
            if (removido != null) {
                "Esquecido: ${removido.text}"
            } else {
                "Não encontrei esse fato na memória."
            }
        }

    return listOf(lembrar, listar, esquecer)
}
