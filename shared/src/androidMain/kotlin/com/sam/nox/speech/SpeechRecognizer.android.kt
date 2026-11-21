package com.sam.nox.speech

/**
 * Stub Android do [SpeechRecognizer].
 *
 * A implementação offline no Android exige integrar whisper.cpp via NDK/JNI
 * (ainda não implementado). Ver [[whisper-android-integration]].
 */
actual class SpeechRecognizer {
    actual suspend fun transcribe(samples: FloatArray, language: String?): TranscriptionResult =
        throw NotImplementedError("SpeechRecognizer ainda não implementado no Android")

    actual fun close() = Unit
}
