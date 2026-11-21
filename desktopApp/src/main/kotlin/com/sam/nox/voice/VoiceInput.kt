package com.sam.nox.voice

/**
 * Entrada de voz (microfone + transcrição). A implementação concreta no desktop
 * reúne o microfone e o Whisper; pode ser nula quando não há modelo disponível.
 * [start] começa a gravar; [stopAndTranscribe] encerra e devolve o texto transcrito.
 */
interface VoiceInput {
    fun start()

    suspend fun stopAndTranscribe(): String
}
