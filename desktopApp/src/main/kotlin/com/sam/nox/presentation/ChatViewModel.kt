package com.sam.nox.presentation

import com.sam.nox.chat.ConfirmDecision
import com.sam.nox.data.ChatRepository
import com.sam.nox.mvi.UiEffect
import com.sam.nox.mvi.UiEffectImpl
import com.sam.nox.mvi.UiEvent
import com.sam.nox.mvi.UiEventImpl
import com.sam.nox.mvi.UiState
import com.sam.nox.mvi.UiStateImpl
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.voice.VoiceInput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel da conversa. Compõe os papéis MVI por delegação (ver `com.sam.nox.mvi`):
 * estado observável ([UiState]), eventos de entrada ([UiEvent]) e efeitos ([UiEffect]).
 * Deriva o [NoxMood]:
 *
 * - campo vazio e mic desligado → Idle; texto no campo ou mic ligado → Listening;
 * - requisição em andamento → Thinking;
 * - resposta chegou → Speaking (1,2s + 22ms/caractere, máx. 4,2s) → volta;
 * - falha → Error.
 *
 * A cutucada ([ChatUiState.poked]) é um booleano separado de 700ms.
 *
 * Estado interno (não exposto):
 * - [lastInputWasVoice]: a última entrada foi por voz (para falar a resposta de volta).
 * - [suppressNextDraftReset]: o próximo [ChatUiEvent.DraftChanged] vem de uma atualização
 *   programática do rascunho (transcrição injetada), não de digitação — não limpar [lastInputWasVoice].
 * - [repository]: criado pela [repositoryFactory] com o confirm desta ViewModel (uma ação que
 *   exige confirmação pausa até o usuário aprovar/recusar).
 * - [pendingDeferred]: confirmação pendente, resolvida por [ChatUiEvent.ResolveConfirmation].
 * - [commandHistory]/[historyIndex]: histórico de envios e índice de navegação (-1 = não navegando).
 *
 * @param scope Escopo de corrotinas que executa as operações assíncronas da tela.
 * @param repositoryFactory Cria o [ChatRepository] recebendo o callback de confirmação desta ViewModel.
 * @param voice Entrada de voz (microfone + transcrição); null quando não há modelo disponível.
 * @param speak Fala um texto em voz alta (TTS); null quando não há síntese de voz.
 */
class ChatViewModel(
    private val scope: CoroutineScope,
    repositoryFactory: (confirm: suspend (toolName: String, preview: String) -> ConfirmDecision) -> ChatRepository,
    private val voice: VoiceInput? = null,
    private val speak: (suspend (String) -> Unit)? = null,
) : UiState<ChatUiState> by UiStateImpl(ChatUiState()),
    UiEvent<ChatUiEvent> by UiEventImpl(),
    UiEffect<ChatUiEffect> by UiEffectImpl() {

    private var lastInputWasVoice = false

    private var suppressNextDraftReset = false

    private val repository: ChatRepository = repositoryFactory(::requestConfirmation)

    private var pendingDeferred: CompletableDeferred<ConfirmDecision>? = null

    private var speakingJob: Job? = null
    private var pokeJob: Job? = null

    private val commandHistory = mutableListOf<String>()

    private var historyIndex = -1

    init {
        setState {
            copy(
                content = content.copy(
                    modelName = repository.modelName,
                    micAvailable = voice != null,
                    messages = listOf(
                        Message(nextId(), Author.Nox, "Oi, Samuel. Eu sou o Nox. Em que posso ajudar?"),
                    ),
                ),
            )
        }
        handleEvents()
    }

    /** Despacha um [ChatUiEvent] para ser processado por esta ViewModel. */
    fun onSendEvent(event: ChatUiEvent) {
        scope.launch { sendEvent(event) }
    }

    private fun handleEvents() {
        scope.launch {
            eventFlow.collect { event ->
                when (event) {
                    is ChatUiEvent.DraftChanged -> onDraftChange(event.value)
                    ChatUiEvent.Send -> send()
                    ChatUiEvent.Reset -> reset()
                    ChatUiEvent.ToggleMic -> toggleMic()
                    ChatUiEvent.Poke -> poke()
                    ChatUiEvent.HistoryPrevious -> historyPrevious()
                    ChatUiEvent.HistoryNext -> historyNext()
                    is ChatUiEvent.SetMoodOverride -> setMoodOverride(event.mood)
                    is ChatUiEvent.ResolveConfirmation -> pendingDeferred?.complete(event.decision)
                }
            }
        }
    }

    /** Atalho para atualizar só o conteúdo (sem overlays de loading/erro). */
    private fun updateContent(block: ChatUiState.() -> ChatUiState) {
        setState { copy(content = content.block()) }
    }

    /**
     * Chamado pelo cliente quando uma ação precisa de confirmação; suspende até decidir.
     * `toolName` faz parte do contrato do callback (o cliente o fornece), mas a UI não
     * precisa dele — o `preview` já descreve a ação ao usuário.
     */
    @Suppress("UnusedParameter")
    private suspend fun requestConfirmation(toolName: String, preview: String): ConfirmDecision {
        val deferred = CompletableDeferred<ConfirmDecision>()
        pendingDeferred = deferred
        updateContent { copy(pendingConfirmation = preview) }
        val decision = deferred.await()
        updateContent { copy(pendingConfirmation = null) }
        pendingDeferred = null
        return decision
    }

    /** Atualiza o rascunho e, se ocioso, reflete Listening/Idle no orbe. */
    private fun onDraftChange(value: String) {
        historyIndex = -1 // digitar sai da navegação do histórico
        if (suppressNextDraftReset) {
            // Mudança programática (transcrição injetada no draft): não trata como digitação.
            suppressNextDraftReset = false
        } else {
            lastInputWasVoice = false // digitou → responder em texto
        }
        updateContent {
            // Digitar não deve interromper uma animação em curso (pensando/falando).
            val newMood =
                if (isSending || mood == NoxMood.Thinking || mood == NoxMood.Speaking) {
                    mood
                } else {
                    restingMood(text = value, micOn = micOn)
                }
            copy(draft = value, mood = newMood)
        }
    }

    /** Seta ↑: traz a mensagem enviada anterior para o campo (como no terminal). */
    private fun historyPrevious() {
        if (commandHistory.isEmpty()) return
        historyIndex =
            when {
                historyIndex == -1 -> commandHistory.lastIndex
                historyIndex > 0 -> historyIndex - 1
                else -> 0
            }
        setDraftFromHistory()
    }

    /** Seta ↓: avança para a mensagem mais recente; no fim, limpa o campo. */
    private fun historyNext() {
        if (historyIndex == -1) return
        historyIndex = if (historyIndex < commandHistory.lastIndex) historyIndex + 1 else -1
        setDraftFromHistory()
    }

    private fun setDraftFromHistory() {
        val text = if (historyIndex == -1) "" else commandHistory[historyIndex]
        updateContent { copy(draft = text) }
    }

    /** Debug: força um estado do orbe (ou null para voltar ao automático). */
    private fun setMoodOverride(mood: NoxMood?) {
        updateContent { copy(moodOverride = mood) }
    }

    /** Cutucada: clique no orbe. Dura 700ms sobre o estado atual. */
    private fun poke() {
        pokeJob?.cancel()
        updateContent { copy(poked = true) }
        pokeJob =
            scope.launch {
                delay(700)
                updateContent { copy(poked = false) }
            }
    }

    /** Liga/desliga o microfone. Ao desligar, transcreve e põe o texto no rascunho. */
    private fun toggleMic() {
        val s = stateFlow.value.content
        val v = voice ?: return
        if (!s.micEnabled && !s.micOn) return

        if (!s.micOn) {
            v.start()
            updateContent { copy(micOn = true, mood = NoxMood.Listening) }
        } else {
            updateContent { copy(micOn = false) }
            scope.launch {
                val text =
                    try {
                        v.stopAndTranscribe().trim()
                    } catch (e: Throwable) {
                        ""
                    }
                if (text.isNotEmpty()) lastInputWasVoice = true
                // A atualização do draft abaixo é programática: evita que o
                // onDraftChange resultante trate isto como digitação e limpe lastInputWasVoice.
                suppressNextDraftReset = true
                updateContent {
                    val merged = (draft.trim() + " " + text).trim()
                    val newMood =
                        if (isSending || mood == NoxMood.Thinking || mood == NoxMood.Speaking) {
                            mood
                        } else {
                            restingMood(text = merged, micOn = false)
                        }
                    copy(draft = merged, mood = newMood)
                }
            }
        }
    }

    /** Envia o rascunho atual. */
    private fun send() {
        val state = stateFlow.value.content
        val text = state.draft.trim()
        if (text.isEmpty() || state.isSending) return

        // Guarda no histórico (evita duplicar a última mensagem igual) e reseta a navegação.
        if (commandHistory.lastOrNull() != text) commandHistory.add(text)
        historyIndex = -1

        speakingJob?.cancel()
        // Enviar sempre desliga o microfone.
        if (state.micOn) {
            runCatching { scope.launch { voice?.stopAndTranscribe() } }
        }

        updateContent {
            copy(
                messages = messages + Message(nextId(), Author.User, text),
                draft = "",
                isSending = true,
                micOn = false,
                mood = NoxMood.Thinking,
            )
        }

        scope.launch {
            if (text.startsWith("/erro", ignoreCase = true)) {
                finishWithError("Sem conexão com o modelo. Verifique sua rede e tente de novo.")
                return@launch
            }
            try {
                val reply = stripMarkdown(repository.send(text).trim())
                finishWithReply(reply.ifEmpty { "(sem resposta)" })
            } catch (e: Throwable) {
                finishWithError(e.message ?: "Falha ao consultar o modelo.")
            }
        }
    }

    /** Libera recursos do repository (fechar HTTP client). */
    fun close() = repository.close()

    /** Começa uma conversa nova. */
    private fun reset() {
        speakingJob?.cancel()
        repository.reset()
        updateContent {
            ChatUiState(
                modelName = modelName,
                micAvailable = micAvailable,
                messages = listOf(
                    Message(nextId(), Author.Nox, "Conversa nova. Pode falar."),
                ),
            )
        }
    }

    private fun finishWithReply(reply: String) {
        updateContent {
            copy(
                messages = messages + Message(nextId(), Author.Nox, reply),
                isSending = false,
                mood = NoxMood.Speaking,
            )
        }
        // Fala a resposta em voz alta apenas quando a entrada veio por voz.
        val falarAgora = lastInputWasVoice && speak != null
        lastInputWasVoice = false
        if (falarAgora) {
            scope.launch { runCatching { speak.invoke(reply) } }
        }
        scheduleBackToRest(speakMillisFor(reply))
    }

    private fun finishWithError(message: String) {
        updateContent {
            copy(
                messages = messages + Message(nextId(), Author.Error, message),
                isSending = false,
                mood = NoxMood.Error,
            )
        }
        scheduleBackToRest(3200)
    }

    /** Após falar/erro, volta a Listening (se há rascunho ou mic) ou Idle. */
    private fun scheduleBackToRest(millis: Long) {
        speakingJob =
            scope.launch {
                delay(millis)
                // Força a saída de Speaking/Error: só não interrompe se um novo envio começou.
                updateContent {
                    if (isSending || mood == NoxMood.Thinking) {
                        this
                    } else {
                        copy(mood = restingMood(draft, micOn))
                    }
                }
            }
    }

    /** Estado de repouso puro: Listening se há texto ou mic ligado; senão Idle. */
    private fun restingMood(text: String, micOn: Boolean): NoxMood =
        if (text.isNotBlank() || micOn) NoxMood.Listening else NoxMood.Idle

    private fun speakMillisFor(text: String): Long = (1200L + text.length * 22L).coerceAtMost(4200L)

    /** Gerador de ids incrementais para as mensagens. */
    companion object {
        private var counter = 0L

        private fun nextId(): Long = ++counter
    }
}

/**
 * Remove a formatação Markdown básica das respostas (negrito **x**, itálico *x*,
 * títulos #, código `x`, links [texto](url)), deixando só o texto limpo, já que a
 * UI não renderiza Markdown.
 */
private fun stripMarkdown(text: String): String {
    var t = text
    t = Regex("""\[([^\]]+)]\((https?://[^)]+)\)""").replace(t) { it.groupValues[1] }
    t = Regex("""(\*\*|__)(.+?)\1""").replace(t) { it.groupValues[2] }
    t = Regex("""(?<!\*)\*(?!\s)([^*\n]+?)\*(?!\*)""").replace(t) { it.groupValues[1] }
    t = Regex("""(?<!_)_(?!\s)([^_\n]+?)_(?!_)""").replace(t) { it.groupValues[1] }
    t = Regex("""`([^`]+)`""").replace(t) { it.groupValues[1] }
    t = Regex("""(?m)^\s{0,3}#{1,6}\s+""").replace(t, "")
    t = Regex("""【[^】]*】""").replace(t, "")
    return t.trim()
}
