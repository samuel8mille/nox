package com.sam.nox.voice

import com.sam.nox.speech.MicrophoneCapture
import com.sam.nox.speech.SpeechRecognizer
import com.sam.nox.speech.WavReader
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Demo de linha de comando para testar o reconhecimento de voz offline (Whisper).
 *
 * Uso (microfone):
 *   ./gradlew :desktopApp:runWhisperDemo --args="/caminho/ggml-base.bin"
 *
 * Uso (arquivo WAV, sem microfone):
 *   ./gradlew :desktopApp:runWhisperDemo --args="/caminho/ggml-base.bin /caminho/audio.wav"
 *
 * Baixe um modelo em: https://huggingface.co/ggerganov/whisper.cpp
 * (ex.: ggml-base.bin — bom equilíbrio entre tamanho e qualidade).
 */
fun main(args: Array<String>) {
    val modelPath =
        args.firstOrNull()
            ?: error("Informe o caminho do modelo GGML como argumento. Ex.: ggml-base.bin")
    val wavPath = args.getOrNull(1)

    println("Carregando modelo: $modelPath ...")
    val recognizer = SpeechRecognizer.load(Path.of(modelPath))

    try {
        val audio =
            if (wavPath != null) {
                println("Lendo arquivo: $wavPath")
                WavReader.read(wavPath)
            } else {
                println("🎙️  Gravando... pressione ENTER para parar.")
                var samples: FloatArray? = null
                val capture = thread { samples = MicrophoneCapture.record() }
                readlnOrNull()
                MicrophoneCapture.stop()
                capture.join()
                samples ?: error("Nenhum áudio capturado")
            }

        println("Transcrevendo ${audio.size} amostras (~${audio.size / 16000}s)...")

        val result =
            runBlocking {
                recognizer.transcribe(audio, language = "pt")
            }

        println("\n📝 Transcrição:\n${result.text}")
    } finally {
        recognizer.close()
    }
}
