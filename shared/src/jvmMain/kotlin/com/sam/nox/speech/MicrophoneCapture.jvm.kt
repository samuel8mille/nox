package com.sam.nox.speech

import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Captura de microfone no Desktop (JVM) via javax.sound.sampled.
 *
 * O Whisper exige áudio PCM mono 16 kHz. Gravamos nesse formato e convertemos
 * as amostras de 16 bits para floats normalizados em [-1, 1].
 */
object MicrophoneCapture {
    private const val SAMPLE_RATE = 16_000f
    private const val SAMPLE_SIZE_BITS = 16
    private const val CHANNELS = 1

    private val format =
        AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            // signed =
            true,
            // bigEndian =
            false,
        )

    @Volatile
    private var recording = false

    /**
     * Grava do microfone até [stop] ser chamado e devolve as amostras em float.
     * Bloqueia a thread atual — rode fora da UI.
     */
    fun record(): FloatArray {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        require(AudioSystem.isLineSupported(info)) { "Microfone não suportado neste sistema" }

        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()
        recording = true

        val buffer = ByteArray(4096)
        val out = ByteArrayOutputStream()
        try {
            while (recording) {
                val read = line.read(buffer, 0, buffer.size)
                if (read > 0) out.write(buffer, 0, read)
            }
        } finally {
            line.stop()
            line.close()
        }

        return pcm16ToFloat(out.toByteArray())
    }

    /** Sinaliza o fim da gravação iniciada por [record]. */
    fun stop() {
        recording = false
    }

    /** Converte PCM 16-bit little-endian para floats normalizados em [-1, 1]. */
    internal fun pcm16ToFloat(bytes: ByteArray): FloatArray {
        val samples = FloatArray(bytes.size / 2)
        for (i in samples.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            val sample = (hi shl 8) or lo
            samples[i] = sample / 32768f
        }
        return samples
    }
}
