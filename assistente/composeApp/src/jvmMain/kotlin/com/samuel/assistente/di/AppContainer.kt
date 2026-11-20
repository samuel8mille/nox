package com.samuel.assistente.di

import com.samuel.assistente.data.AnthropicChatRepository
import com.samuel.assistente.data.ChatRepository
import com.samuel.assistente.data.remote.AnthropicApi
import com.samuel.assistente.input.AssistantEventBus
import com.samuel.assistente.input.InputSource
import com.samuel.assistente.input.voice.SpeechRecognizer
import com.samuel.assistente.input.voice.VoskSpeechRecognizer
import java.nio.file.Path

/** Injeção de dependência manual. Dá para trocar por Koin mais tarde sem mexer nas camadas. */
class AppContainer : AutoCloseable {
    private val appDataDir: Path =
        Path.of(System.getProperty("user.home"), "Library", "Application Support", "MinhaAssistente")

    private val api = AnthropicApi()
    val chatRepository: ChatRepository = AnthropicChatRepository(api)
    val speechRecognizer: SpeechRecognizer = VoskSpeechRecognizer(modelsDir = appDataDir.resolve("models"))
    val eventBus = AssistantEventBus()

    /** Adicione aqui GlobalHotkeySource etc. */
    private val inputSources: List<InputSource> = emptyList()

    fun start() = inputSources.forEach { it.start(eventBus) }

    override fun close() {
        inputSources.forEach { it.close() }
        speechRecognizer.close()
        api.close()
    }
}
