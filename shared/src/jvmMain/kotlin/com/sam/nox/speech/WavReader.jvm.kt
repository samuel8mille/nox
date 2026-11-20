package com.sam.nox.speech

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

/**
 * Lê um arquivo WAV e devolve as amostras como floats mono 16 kHz,
 * no formato que o Whisper espera. Útil para testar transcrição sem microfone.
 */
object WavReader {

    private const val TARGET_RATE = 16_000f

    fun read(path: String): FloatArray {
        var stream = AudioSystem.getAudioInputStream(File(path))

        // Converte para PCM 16-bit mono 16 kHz, se necessário.
        val target = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            TARGET_RATE,
            16,
            1,
            2,
            TARGET_RATE,
            false,
        )
        if (!AudioSystem.isConversionSupported(target, stream.format)) {
            error("Conversão de áudio não suportada para o formato ${stream.format}")
        }
        stream = AudioSystem.getAudioInputStream(target, stream)

        val bytes = stream.readAllBytes()
        return MicrophoneCapture.pcm16ToFloat(bytes)
    }
}
