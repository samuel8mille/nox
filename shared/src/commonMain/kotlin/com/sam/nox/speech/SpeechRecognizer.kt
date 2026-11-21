package com.sam.nox.speech

/**
 * Resultado de uma transcrição de voz.
 */
data class TranscriptionResult(val text: String, val language: String? = null)

/**
 * Reconhecedor de voz offline baseado no Whisper.
 *
 * A implementação concreta é fornecida por plataforma (expect/actual).
 * No JVM/Desktop usa o binding whisper-jni (whisper.cpp).
 */
expect class SpeechRecognizer {
    /**
     * Transcreve o áudio PCM mono 16 kHz (floats normalizados em [-1, 1]).
     *
     * @param samples amostras de áudio em 16 kHz, mono.
     * @param language código ISO do idioma (ex.: "pt") ou null para detecção automática.
     */
    suspend fun transcribe(samples: FloatArray, language: String? = null): TranscriptionResult

    /** Libera os recursos nativos. */
    fun close()
}
