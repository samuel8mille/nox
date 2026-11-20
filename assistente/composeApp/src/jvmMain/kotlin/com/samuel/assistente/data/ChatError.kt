package com.samuel.assistente.data

/** Erros que a camada de dados expõe, já com mensagem pronta para o usuário. */
sealed class ChatError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingApiKey : ChatError(
        "A variável de ambiente ANTHROPIC_API_KEY não está definida. " +
            "Defina-a e reinicie o app."
    )

    class InvalidApiKey(detail: String?) : ChatError(
        "Chave de API inválida ou sem permissão (${detail ?: "401/403"}). " +
            "Confira o valor de ANTHROPIC_API_KEY."
    )

    class Network(cause: Throwable) : ChatError(
        "Sem conexão com a API. Verifique sua internet e tente de novo.",
        cause,
    )

    class RateLimited : ChatError("Limite de requisições atingido. Aguarde um pouco e tente de novo.")

    class Overloaded(status: Int) : ChatError("A API está sobrecarregada ou indisponível ($status). Tente de novo em instantes.")

    class Refused : ChatError("O modelo recusou responder a essa mensagem.")

    class Api(status: Int, detail: String?) : ChatError("Erro da API ($status): ${detail ?: "sem detalhes"}")

    class Unexpected(cause: Throwable) : ChatError("Erro inesperado: ${cause.message ?: cause::class.simpleName}", cause)
}
