package com.sam.nox.speech

/**
 * Stub iOS do [SpeechRecognizer].
 *
 * A implementação offline no iOS exige integrar whisper.cpp via cinterop
 * (ainda não implementado).
 */
actual class SpeechRecognizer {
    actual suspend fun transcribe(
        samples: FloatArray,
        language: String?,
    ): TranscriptionResult =
        throw NotImplementedError("SpeechRecognizer ainda não implementado no iOS")

    actual fun close() = Unit
}
