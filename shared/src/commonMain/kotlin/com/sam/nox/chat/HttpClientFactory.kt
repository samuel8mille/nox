package com.sam.nox.chat

import io.ktor.client.HttpClient

/** Cria o HttpClient com o engine adequado a cada plataforma (OkHttp no JVM/Android, Darwin no iOS). */
expect fun createHttpClient(): HttpClient
