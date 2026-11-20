package com.sam.nox.chat

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Uma ferramenta que a assistente pode invocar (tool calling).
 *
 * @param name nome único da função (ex.: "obter_hora").
 * @param description o que a função faz — o modelo usa isto pra decidir quando chamá-la.
 * @param parameters JSON Schema dos parâmetros (objeto com "type", "properties", "required").
 *                   Use [noParameters] se a função não recebe argumentos.
 * @param requiresConfirmation se true, a ação altera algo externo (GitHub, arquivos,
 *                   sistema) e deve ser confirmada pelo usuário antes de executar.
 * @param preview gera um texto legível descrevendo o que a ação fará, a partir dos
 *                argumentos — exibido no pedido de confirmação. Null = usa a descrição.
 * @param execute executa a função com os argumentos que o modelo forneceu e devolve
 *                o resultado como texto (que volta ao modelo).
 */
class Tool(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val requiresConfirmation: Boolean = false,
    val preview: ((args: Map<String, JsonElement>) -> String)? = null,
    val execute: suspend (args: Map<String, JsonElement>) -> String,
)

/** Schema para uma ferramenta sem parâmetros. */
val noParameters: JsonObject = JsonObject(
    mapOf(
        "type" to kotlinx.serialization.json.JsonPrimitive("object"),
        "properties" to JsonObject(emptyMap()),
    ),
)
