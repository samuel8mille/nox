package com.sam.nox.voice

import com.sam.nox.AudioRecorder
import com.sam.nox.ChatEngine
import com.sam.nox.NoxController
import com.sam.nox.Speaker
import com.sam.nox.Transcriber
import com.sam.nox.chat.OpenRouterClient
import com.sam.nox.chat.builtInTools
import com.sam.nox.chat.webSearchTool
import com.sam.nox.data.ChatRepository
import com.sam.nox.speech.MicrophoneCapture
import com.sam.nox.speech.SpeechRecognizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Captura de microfone no Desktop, adaptando [MicrophoneCapture] (bloqueante)
 * à interface [AudioRecorder] do controller.
 *
 * A gravação roda numa thread dedicada; [stop] sinaliza o fim e aguarda as amostras.
 */
class DesktopAudioRecorder : AudioRecorder {
    private var samples: CompletableDeferred<FloatArray>? = null
    private var worker: Thread? = null

    override fun start() {
        val deferred = CompletableDeferred<FloatArray>()
        samples = deferred
        worker =
            thread(name = "nox-mic") {
                try {
                    deferred.complete(MicrophoneCapture.record())
                } catch (e: Throwable) {
                    deferred.completeExceptionally(e)
                }
            }
    }

    override suspend fun stop(): FloatArray {
        MicrophoneCapture.stop()
        val result = samples?.await() ?: FloatArray(0)
        worker?.join()
        worker = null
        samples = null
        return result
    }
}

/** Adaptador do [SpeechRecognizer] (Whisper) para a interface [Transcriber]. */
class DesktopTranscriber(private val recognizer: SpeechRecognizer, private val language: String? = "pt") :
    Transcriber {
    override suspend fun transcribe(samples: FloatArray): String = recognizer.transcribe(samples, language).text
}

/** Adaptador do [OpenRouterClient] para a interface [ChatEngine]. */
class DesktopChatEngine(private val llm: OpenRouterClient) : ChatEngine {
    override suspend fun reply(userText: String): String = llm.send(userText)
}

/**
 * TTS simples no macOS via comando `say` (offline, sem dependências).
 * Em outros sistemas, ou se `say` não existir, fala vira no-op silencioso.
 */
class SystemSpeaker(private val voice: String? = null) : Speaker {
    override suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext
        try {
            val cmd =
                buildList {
                    add("say")
                    if (voice != null) {
                        add("-v")
                        add(voice)
                    }
                    add(text)
                }
            ProcessBuilder(cmd).inheritIO().start().waitFor()
            Unit
        } catch (_: Throwable) {
            // `say` indisponível (não-macOS): ignora silenciosamente.
        }
    }
}

/**
 * Monta o [NoxController] para o Desktop com todas as peças reais:
 * microfone + Whisper + OpenRouter + TTS do sistema.
 *
 * @param scope escopo de corrotina (ex.: rememberCoroutineScope na UI).
 * @param modelPath caminho do modelo GGML do Whisper (ex.: ggml-base.bin).
 * @param apiKey API key do OpenRouter (padrão: OPENROUTER_API_KEY / -Dopenrouter.key).
 */
fun createDesktopNox(
    scope: CoroutineScope,
    modelPath: Path,
    apiKey: String = ChatRepository.resolveApiKey(),
    model: String = "qwen/qwen3.8-27b",
    onError: (Throwable) -> Unit = {},
): NoxController {
    val recognizer = SpeechRecognizer.load(modelPath)
    val tavilyKey = ChatRepository.resolveTavilyKey()
    val tools =
        if (tavilyKey.isNullOrBlank()) {
            builtInTools
        } else {
            builtInTools + webSearchTool(apiKey = tavilyKey)
        }
    val llm =
        OpenRouterClient(
            apiKey = apiKey,
            model = model,
            tools = tools,
            fallbackModels = listOf("openai/gpt-oss-20b", "openai/gpt-oss-120b"),
            baseUrl = "https://api.groq.com/openai/v1",
        )

    return NoxController(
        scope = scope,
        recorder = DesktopAudioRecorder(),
        transcriber = DesktopTranscriber(recognizer),
        chat = DesktopChatEngine(llm),
        speaker = SystemSpeaker(),
        onError = onError,
    )
}

/**
 * Entrada de voz do chat: microfone + Whisper. Liga o botão de microfone do
 * compositor ao pipeline real de transcrição.
 */
class DesktopVoiceInput(
    private val recorder: DesktopAudioRecorder = DesktopAudioRecorder(),
    private val transcriber: DesktopTranscriber,
) : VoiceInput {
    override fun start() = recorder.start()

    override suspend fun stopAndTranscribe(): String {
        val samples = recorder.stop()
        if (samples.isEmpty()) return ""
        return transcriber.transcribe(samples)
    }
}

/**
 * Cria a entrada de voz se houver modelo Whisper; devolve null caso contrário
 * (o botão de microfone fica indisponível).
 */
fun createVoiceInput(modelPath: Path?): VoiceInput? {
    if (modelPath == null) return null
    return try {
        val recognizer = SpeechRecognizer.load(modelPath)
        DesktopVoiceInput(transcriber = DesktopTranscriber(recognizer))
    } catch (_: Throwable) {
        null
    }
}
