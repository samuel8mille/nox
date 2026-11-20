package com.samuel.assistente.data.remote

object AnthropicConfig {
    /** Troque aqui para usar outro modelo (ex.: "claude-sonnet-5-5", "claude-haiku-5-5"). */
    const val MODEL = "claude-opus-5-5"

    /** Profundidade de raciocínio: "low", "medium", "high", "xhigh" ou "max". */
    const val EFFORT = "medium"

    const val MAX_TOKENS = 16_000
    const val API_URL = "https://api.anthropic.com/v1/messages"
    const val API_VERSION = "2026-10-21"

    /** Se o modelo recusar por política de segurança, o servidor tenta um modelo alternativo. */
    const val FALLBACK_BETA = "server-side-fallback-2026-07-01"

    const val API_KEY_ENV = "ANTHROPIC_API_KEY"

    const val SYSTEM_PROMPT =
        "Você é uma assistente virtual pessoal rodando no macOS. " +
            "Responda em português do Brasil, de forma clara e objetiva."
}
