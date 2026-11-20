package com.sam.nox.chat

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Monta um JSON Schema de objeto a partir de pares propriedade→definição. */
private fun objectSchema(
    properties: Map<String, JsonObject>,
    required: List<String> = emptyList(),
): JsonObject = JsonObject(
    buildMap {
        put("type", JsonPrimitive("object"))
        put("properties", JsonObject(properties))
        if (required.isNotEmpty()) {
            put("required", kotlinx.serialization.json.JsonArray(required.map { JsonPrimitive(it) }))
        }
    },
)

private fun prop(type: String, description: String): JsonObject = JsonObject(
    mapOf("type" to JsonPrimitive(type), "description" to JsonPrimitive(description)),
)

/** Ferramenta: devolve a data e hora atuais do dispositivo. */
val horaAtualTool = Tool(
    name = "obter_hora_atual",
    description = "Retorna a data e a hora atuais reais do sistema (dia, mês, ano, dia da " +
        "semana e horas). Use sempre que o usuário perguntar que horas são, que dia é hoje, " +
        "a data, o dia da semana ou qualquer coisa sobre o momento atual.",
    parameters = noParameters,
) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val hora = now.hour.toString().padStart(2, '0')
    val minuto = now.minute.toString().padStart(2, '0')
    val diaSemana = when (now.dayOfWeek) {
        DayOfWeek.MONDAY -> "segunda-feira"
        DayOfWeek.TUESDAY -> "terça-feira"
        DayOfWeek.WEDNESDAY -> "quarta-feira"
        DayOfWeek.THURSDAY -> "quinta-feira"
        DayOfWeek.FRIDAY -> "sexta-feira"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
        else -> ""
    }
    "Hoje é $diaSemana, ${now.dayOfMonth}/${now.monthNumber}/${now.year}. Hora atual: $hora:$minuto."
}

/** Ferramenta: calcula uma operação aritmética simples entre dois números. */
val calculadoraTool = Tool(
    name = "calcular",
    description = "Faz uma operação aritmética entre dois números. Operações: soma, subtracao, multiplicacao, divisao.",
    parameters = objectSchema(
        properties = mapOf(
            "operacao" to prop("string", "Uma de: soma, subtracao, multiplicacao, divisao"),
            "a" to prop("number", "Primeiro número"),
            "b" to prop("number", "Segundo número"),
        ),
        required = listOf("operacao", "a", "b"),
    ),
) { args ->
    val op = (args["operacao"] as? JsonPrimitive)?.contentOrNull
    val a = (args["a"] as? JsonPrimitive)?.doubleOrNull
    val b = (args["b"] as? JsonPrimitive)?.doubleOrNull
    if (a == null || b == null || op == null) {
        "Erro: argumentos inválidos."
    } else when (op) {
        "soma" -> "${a + b}"
        "subtracao" -> "${a - b}"
        "multiplicacao" -> "${a * b}"
        "divisao" -> if (b == 0.0) "Erro: divisão por zero." else "${a / b}"
        else -> "Erro: operação desconhecida '$op'."
    }
}

/** Conjunto de ferramentas prontas para a assistente. */
val builtInTools: List<Tool> = listOf(horaAtualTool, calculadoraTool)
