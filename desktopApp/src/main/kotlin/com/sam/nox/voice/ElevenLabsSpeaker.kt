package com.sam.nox

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

/**
 * TTS neural via API da ElevenLabs. A resposta do LLM (texto) é enviada à API,
 * que devolve áudio PCM 16-bit mono a 24 kHz; tocamos com `javax.sound.sampled`
 * (sem dependências extras — nada de decodificar mp3).
 *
 * A chave vem de `elevenlabs.key` (system property, setada pelo build a partir
 * de ELEVENLABS_API_KEY) ou da env `ELEVENLABS_API_KEY`. Sem chave → no-op.
 */
class ElevenLabsSpeaker(
    private val http: HttpClient,
    apiKey: String? = resolveKey(),
    private val voiceId: String = resolveVoice(),
    private val modelId: String = "eleven_multilingual_v2",
) : Speaker {

    private val key: String? = apiKey?.takeIf { it.isNotBlank() }

    val available: Boolean get() = key != null

    override suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        val k = key ?: return@withContext
        if (text.isBlank()) return@withContext
        try {
            val pcm = fetchPcm(text, k) ?: return@withContext
            play(pcm)
        } catch (_: Throwable) {
            // Falha de rede/áudio: silenciosa (o texto já está na tela).
        }
    }

    private suspend fun fetchPcm(text: String, apiKey: String): ByteArray? {
        // pcm_24000 = PCM 16-bit, mono, 24 kHz, little-endian.
        val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId?output_format=pcm_24000"
        val body = Json.encodeToString(
            TtsRequest.serializer(),
            TtsRequest(text = text, modelId = modelId),
        )
        val resp: HttpResponse = http.post(url) {
            headers { append("xi-api-key", apiKey) }
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (!resp.status.isSuccess()) return null
        return resp.readRawBytes()
    }

    private fun play(pcm: ByteArray) {
        val format = AudioFormat(24_000f, 16, 1, true, false) // 24kHz, 16-bit, mono, signed, LE
        val info = DataLine.Info(SourceDataLine::class.java, format)
        if (!AudioSystem.isLineSupported(info)) return
        val line = AudioSystem.getLine(info) as SourceDataLine
        line.open(format)
        line.start()
        try {
            var offset = 0
            val chunk = 4096
            while (offset < pcm.size) {
                val len = minOf(chunk, pcm.size - offset)
                line.write(pcm, offset, len)
                offset += len
            }
            line.drain()
        } finally {
            line.stop()
            line.close()
        }
    }

    @Serializable
    private data class TtsRequest(
        val text: String,
        @SerialName("model_id") val modelId: String,
    )

    companion object {
        fun resolveKey(): String? =
            System.getProperty("elevenlabs.key") ?: System.getenv("ELEVENLABS_API_KEY")

        // Voz padrão (pode ser sobrescrita por elevenlabs.voice / ELEVENLABS_VOICE_ID).
        // "Sarah" — voz feminina natural, boa em PT-BR no modelo multilingual.
        fun resolveVoice(): String =
            System.getProperty("elevenlabs.voice")
                ?: System.getenv("ELEVENLABS_VOICE_ID")
                ?: "EXAVITQu4vr4xnSDxMaL"
    }
}
