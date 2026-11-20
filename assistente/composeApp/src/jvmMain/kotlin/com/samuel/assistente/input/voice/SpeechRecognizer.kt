package com.samuel.assistente.input.voice

import kotlinx.coroutines.flow.Flow

sealed interface SpeechEvent {
    /** Preparando o reconhecimento (ex.: baixando ou carregando o modelo). */
    data class Preparing(val message: String) : SpeechEvent

    /** Microfone aberto, gravando. */
    data object Listening : SpeechEvent

    /** Transcrição parcial, atualizada enquanto a pessoa fala. */
    data class Partial(val text: String) : SpeechEvent

    /** Transcrição final. O Flow termina logo depois. */
    data class Final(val text: String) : SpeechEvent
}

class SpeechError(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Fala para texto. Implementação atual: [VoskSpeechRecognizer] (offline). */
interface SpeechRecognizer : AutoCloseable {
    /**
     * Grava até detectar o fim da fala, até [stopListening] ser chamado ou até o tempo máximo.
     * Emite [SpeechEvent]s e termina com [SpeechEvent.Final]. Falhas chegam como [SpeechError].
     */
    fun listen(): Flow<SpeechEvent>

    /** Encerra a gravação atual. A transcrição do que já foi dito é entregue normalmente. */
    fun stopListening()
}
