package com.sam.nox

import com.sam.nox.ui.orb.NoxMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Captura de áudio do microfone. A implementação é por plataforma.
 *
 * O fluxo esperado é: [start] começa a gravar; [stop] encerra e devolve as
 * amostras PCM mono 16 kHz em floats normalizados [-1, 1] — o formato do Whisper.
 */
interface AudioRecorder {
    fun start()

    suspend fun stop(): FloatArray
}

/** Converte áudio em texto (ex.: Whisper). */
interface Transcriber {
    suspend fun transcribe(samples: FloatArray): String
}

/** Gera a resposta da assistente a partir do texto do usuário (ex.: LLM/Ollama). */
interface ChatEngine {
    suspend fun reply(userText: String): String
}

/** Síntese de voz da resposta (ex.: TTS). Pode ser um stub silencioso. */
interface Speaker {
    suspend fun speak(text: String)
}

/** Resultado de um turno de conversa, para exibir no histórico/UI. */
data class Turn(val userText: String, val assistantText: String)

/**
 * Orquestra o pipeline da Nox e expõe o [NoxMood] que anima o orbe:
 *
 * Idle → (usuário toca o mic) → Listening → Thinking → Speaking → Idle
 *
 * As peças ([recorder], [transcriber], [chat], [speaker]) são injetadas por
 * plataforma, mantendo esta lógica multiplataforma e testável.
 *
 * @property state Estado atual da assistente — o orbe observa isto para reagir.
 * @property lastTurn Último turno concluído (para exibir transcrição/resposta, se quiser).
 * @property isBusy True quando está no meio de um turno (gravando ou processando).
 */
class NoxController(
    private val scope: CoroutineScope,
    private val recorder: AudioRecorder,
    private val transcriber: Transcriber,
    private val chat: ChatEngine,
    private val speaker: Speaker,
    private val onError: (Throwable) -> Unit = {},
) {
    private val _state = MutableStateFlow(NoxMood.Idle)
    val state: StateFlow<NoxMood> = _state.asStateFlow()

    private val _lastTurn = MutableStateFlow<Turn?>(null)
    val lastTurn: StateFlow<Turn?> = _lastTurn.asStateFlow()

    private var job: Job? = null

    val isBusy: Boolean get() = _state.value != NoxMood.Idle

    /**
     * Inicia um turno: começa a ouvir (gravar o microfone).
     * Chame [stopListening] para encerrar a captura e processar.
     */
    fun startListening() {
        if (_state.value != NoxMood.Idle) return
        try {
            recorder.start()
            _state.value = NoxMood.Listening
        } catch (e: Throwable) {
            onError(e)
            _state.value = NoxMood.Idle
        }
    }

    /**
     * Encerra a captura e dispara o restante do pipeline:
     * transcrição → LLM → fala, atualizando o estado em cada etapa.
     */
    fun stopListening() {
        if (_state.value != NoxMood.Listening) return
        job =
            scope.launch {
                try {
                    val samples = recorder.stop()

                    _state.value = NoxMood.Thinking
                    val userText = transcriber.transcribe(samples).trim()
                    if (userText.isEmpty()) {
                        _state.value = NoxMood.Idle
                        return@launch
                    }

                    val answer = chat.reply(userText).trim()
                    _lastTurn.value = Turn(userText, answer)

                    _state.value = NoxMood.Speaking
                    if (answer.isNotEmpty()) speaker.speak(answer)
                } catch (e: Throwable) {
                    onError(e)
                } finally {
                    _state.value = NoxMood.Idle
                }
            }
    }

    /** Alterna entre começar e parar de ouvir — conveniente para um botão único. */
    fun toggleListening() {
        when (_state.value) {
            NoxMood.Idle -> startListening()
            NoxMood.Listening -> stopListening()
            else -> Unit // ocupado processando/falando: ignora
        }
    }

    /** Cancela qualquer trabalho em andamento e volta a Idle. */
    fun cancel() {
        job?.cancel()
        job = null
        _state.value = NoxMood.Idle
    }
}
