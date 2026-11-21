package com.sam.nox.speech

import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

/**
 * Implementação JVM/Desktop do [SpeechRecognizer] usando whisper-jni (whisper.cpp).
 *
 * @param modelPath caminho para o modelo GGML quantizado (ex.: ggml-base.bin).
 *                  Baixe em: https://huggingface.co/ggerganov/whisper.cpp
 */
actual class SpeechRecognizer private constructor(
    private val whisper: WhisperJNI,
    private val context: WhisperContext,
) {
    actual suspend fun transcribe(samples: FloatArray, language: String?): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val params =
                WhisperFullParams().apply {
                    if (language != null) this.language = language
                    printProgress = false
                    printRealtime = false
                }

            val result = whisper.full(context, params, samples, samples.size)
            if (result != 0) {
                error("Falha na transcrição do Whisper (código $result)")
            }

            val segments = whisper.fullNSegments(context)
            val text =
                buildString {
                    for (i in 0 until segments) {
                        append(whisper.fullGetSegmentText(context, i))
                    }
                }.trim()

            TranscriptionResult(text = text, language = language)
        }

    actual fun close() {
        context.close()
    }

    companion object {
        /**
         * Carrega um modelo Whisper a partir do caminho informado.
         * Deve ser chamado uma única vez (o contexto é caro de criar).
         */
        fun load(modelPath: Path): SpeechRecognizer {
            val whisper = WhisperJNI()
            WhisperJNI.loadLibrary()
            WhisperJNI.setLibraryLogger(null)
            val context =
                whisper.init(modelPath)
                    ?: error("Não foi possível carregar o modelo Whisper em $modelPath")
            return SpeechRecognizer(whisper, context)
        }
    }
}
