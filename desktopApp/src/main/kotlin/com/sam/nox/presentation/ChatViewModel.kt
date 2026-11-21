package com.sam.nox.presentation

import com.sam.nox.data.ChatRepository
import com.sam.nox.ui.orb.NoxMood
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Quem enviou a mensagem. */
enum class Author { User, Nox, Error }

/** Uma mensagem exibida na conversa. */
data class Message(
    val id: Long,
    val author: Author,
    val text: String,
)

/**
 * Entrada de voz (microfone + transcrição). A implementação concreta no desktop
 * reúne o microfone e o Whisper; pode ser nula quando não há modelo disponível.
 */
interface VoiceInput {
    /** Começa a gravar o microfone. */
    fun start()

    /** Encerra a gravação e devolve o texto transcrito. */
    suspend fun stopAndTranscribe(): String
}

/** Estado observável da tela de chat. */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val mood: NoxMood = NoxMood.Idle,
    val isSending: Boolean = false,
    val modelName: String = "",
    /** Microfone capturando agora. */
    val micOn: Boolean = false,
    /** True quando há entrada de voz disponível (modelo Whisper carregado). */
    val micAvailable: Boolean = false,
    /** True por ~700ms após o clique no orbe (cutucada). */
    val poked: Boolean = false,
    /** Modo debug: quando != null, força o estado do orbe (ignora o fluxo real). */
    val moodOverride: NoxMood? = null,
    /** Ação aguardando confirmação do usuário (ex.: criar repo). Null = nenhuma. */
    val pendingConfirmation: String? = null,
) {
    /** Estado efetivo que o orbe desenha: o override do debug ou o derivado. */
    val effectiveMood: NoxMood get() = moodOverride ?: mood

    /** Enviar desabilitado quando vazio, ou durante requisição/transcrição. */
    val sendEnabled: Boolean get() = draft.isNotBlank() && !isSending
    /** Microfone bloqueado durante Thinking/Speaking. */
    val micEnabled: Boolean get() = micAvailable && !isSending && mood != NoxMood.Speaking

    val placeholder: String get() = if (micOn) "Ouvindo… fale ou digite" else "Fale com o Nox…"
    val statusHint: String get() = when {
        moodOverride != null -> "Debug: estado forçado"
        micOn -> "Ouvindo pelo microfone"
        else -> when (mood) {
            NoxMood.Idle -> "Pergunte qualquer coisa"
            NoxMood.Listening -> "Acompanhando o que você digita"
            NoxMood.Thinking -> "Consultando o modelo"
            NoxMood.Speaking -> "Resposta recebida"
            NoxMood.Error -> "Veja os detalhes na conversa"
        }
    }
}

/**
 * ViewModel da conversa. Expõe [uiState] como [StateFlow] e deriva o [NoxMood]:
 *
 * - campo vazio e mic desligado → Idle; texto no campo ou mic ligado → Listening;
 * - requisição em andamento → Thinking;
 * - resposta chegou → Speaking (1,2s + 22ms/caractere, máx. 4,2s) → volta;
 * - falha → Error.
 *
 * A cutucada ([ChatUiState.poked]) é um booleano separado de 700ms.
 */
class ChatViewModel(
    private val scope: CoroutineScope,
    repositoryFactory: (confirm: suspend (String) -> Boolean) -> ChatRepository,
    private val voice: VoiceInput? = null,
    /** Fala um texto em voz alta (TTS). Null = sem síntese de voz. */
    private val speak: (suspend (String) -> Unit)? = null,
) {
    /** True quando a próxima mensagem foi ditada por voz (para falar a resposta de volta). */
    private var lastInputWasVoice = false

    /**
     * True quando o próximo `onDraftChange` vem de uma atualização programática do
     * rascunho (ex.: transcrição de voz injetada no campo) e não de digitação do
     * usuário — nesse caso não devemos limpar [lastInputWasVoice].
     */
    private var suppressNextDraftReset = false
    // O repository recebe o confirm deste ViewModel, resolvendo a dependência:
    // uma ação que exige confirmação pausa aqui até o usuário aprovar/recusar.
    private val repository: ChatRepository = repositoryFactory(::requestConfirmation)

    /** Deferred da confirmação atualmente pendente (resolvido por approve/reject). */
    private var pendingDeferred: CompletableDeferred<Boolean>? = null

    private val _uiState = MutableStateFlow(
        ChatUiState(
            modelName = repository.modelName,
            micAvailable = voice != null,
            messages = listOf(
                Message(nextId(), Author.Nox, "Oi, Samuel. Eu sou o Nox. Em que posso ajudar?"),
            ),
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var speakingJob: Job? = null
    private var pokeJob: Job? = null

    /** Chamado pelo cliente quando uma ação precisa de confirmação; suspende até decidir. */
    private suspend fun requestConfirmation(preview: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingDeferred = deferred
        _uiState.update { it.copy(pendingConfirmation = preview) }
        val approved = deferred.await()
        _uiState.update { it.copy(pendingConfirmation = null) }
        pendingDeferred = null
        return approved
    }

    /** UI: usuário aprovou a ação pendente. */
    fun approveAction() { pendingDeferred?.complete(true) }

    /** UI: usuário recusou a ação pendente. */
    fun rejectAction() { pendingDeferred?.complete(false) }

    /** Histórico das mensagens enviadas (mais antiga → mais recente) e índice de navegação. */
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1 // -1 = não navegando (rascunho atual)

    /** Atualiza o rascunho e, se ocioso, reflete Listening/Idle no orbe. */
    fun onDraftChange(value: String) {
        historyIndex = -1 // digitar sai da navegação do histórico
        if (suppressNextDraftReset) {
            // Mudança programática (transcrição injetada no draft): não trata como digitação.
            suppressNextDraftReset = false
        } else {
            lastInputWasVoice = false // digitou → responder em texto
        }
        _uiState.update { s ->
            // Digitar não deve interromper uma animação em curso (pensando/falando).
            val mood = if (s.isSending || s.mood == NoxMood.Thinking || s.mood == NoxMood.Speaking) {
                s.mood
            } else {
                restingMood(text = value, micOn = s.micOn)
            }
            s.copy(draft = value, mood = mood)
        }
    }

    /** Seta ↑: traz a mensagem enviada anterior para o campo (como no terminal). */
    fun historyPrevious() {
        if (commandHistory.isEmpty()) return
        historyIndex = when {
            historyIndex == -1 -> commandHistory.lastIndex
            historyIndex > 0 -> historyIndex - 1
            else -> 0
        }
        setDraftFromHistory()
    }

    /** Seta ↓: avança para a mensagem mais recente; no fim, limpa o campo. */
    fun historyNext() {
        if (historyIndex == -1) return
        historyIndex = if (historyIndex < commandHistory.lastIndex) historyIndex + 1 else -1
        setDraftFromHistory()
    }

    private fun setDraftFromHistory() {
        val text = if (historyIndex == -1) "" else commandHistory[historyIndex]
        _uiState.update { it.copy(draft = text) }
    }

    /** Debug: força um estado do orbe (ou null para voltar ao automático). */
    fun setMoodOverride(mood: NoxMood?) {
        _uiState.update { it.copy(moodOverride = mood) }
    }

    /** Cutucada: clique no orbe. Dura 700ms sobre o estado atual. */
    fun poke() {
        pokeJob?.cancel()
        _uiState.update { it.copy(poked = true) }
        pokeJob = scope.launch {
            delay(700)
            _uiState.update { it.copy(poked = false) }
        }
    }

    /** Liga/desliga o microfone. Ao desligar, transcreve e põe o texto no rascunho. */
    fun toggleMic() {
        val s = _uiState.value
        val v = voice ?: return
        if (!s.micEnabled && !s.micOn) return

        if (!s.micOn) {
            v.start()
            _uiState.update { it.copy(micOn = true, mood = NoxMood.Listening) }
        } else {
            _uiState.update { it.copy(micOn = false) }
            scope.launch {
                val text = try {
                    v.stopAndTranscribe().trim()
                } catch (e: Throwable) {
                    ""
                }
                if (text.isNotEmpty()) lastInputWasVoice = true
                // A atualização do draft abaixo é programática: evita que o
                // onDraftChange resultante trate isto como digitação e limpe lastInputWasVoice.
                suppressNextDraftReset = true
                _uiState.update { st ->
                    val merged = (st.draft.trim() + " " + text).trim()
                    val mood = if (st.isSending || st.mood == NoxMood.Thinking || st.mood == NoxMood.Speaking) {
                        st.mood
                    } else {
                        restingMood(text = merged, micOn = false)
                    }
                    st.copy(draft = merged, mood = mood)
                }
            }
        }
    }

    /** Envia o rascunho atual. */
    fun send() {
        val state = _uiState.value
        val text = state.draft.trim()
        if (text.isEmpty() || state.isSending) return

        // Guarda no histórico (evita duplicar a última mensagem igual) e reseta a navegação.
        if (commandHistory.lastOrNull() != text) commandHistory.add(text)
        historyIndex = -1

        speakingJob?.cancel()
        // Enviar sempre desliga o microfone.
        if (state.micOn) { runCatching { scope.launch { voice?.stopAndTranscribe() } } }

        _uiState.update {
            it.copy(
                messages = it.messages + Message(nextId(), Author.User, text),
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
    fun reset() {
        speakingJob?.cancel()
        repository.reset()
        _uiState.update {
            ChatUiState(
                modelName = repository.modelName,
                micAvailable = it.micAvailable,
                messages = listOf(
                    Message(nextId(), Author.Nox, "Conversa nova. Pode falar."),
                ),
            )
        }
    }

    private fun finishWithReply(reply: String) {
        _uiState.update {
            it.copy(
                messages = it.messages + Message(nextId(), Author.Nox, reply),
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
        _uiState.update {
            it.copy(
                messages = it.messages + Message(nextId(), Author.Error, message),
                isSending = false,
                mood = NoxMood.Error,
            )
        }
        scheduleBackToRest(3200)
    }

    /** Após falar/erro, volta a Listening (se há rascunho ou mic) ou Idle. */
    private fun scheduleBackToRest(millis: Long) {
        speakingJob = scope.launch {
            delay(millis)
            // Força a saída de Speaking/Error: só não interrompe se um novo envio começou.
            _uiState.update { s ->
                if (s.isSending || s.mood == NoxMood.Thinking) s
                else s.copy(mood = restingMood(s.draft, s.micOn))
            }
        }
    }

    /** Estado de repouso puro: Listening se há texto ou mic ligado; senão Idle. */
    private fun restingMood(text: String, micOn: Boolean): NoxMood =
        if (text.isNotBlank() || micOn) NoxMood.Listening else NoxMood.Idle

    private fun speakMillisFor(text: String): Long =
        (1200L + text.length * 22L).coerceAtMost(4200L)

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
    // Links [texto](url) → texto
    t = Regex("""\[([^\]]+)]\((https?://[^)]+)\)""").replace(t) { it.groupValues[1] }
    // Negrito/itálico: **x**, __x__, *x*, _x_ → x
    t = Regex("""(\*\*|__)(.+?)\1""").replace(t) { it.groupValues[2] }
    t = Regex("""(?<!\*)\*(?!\s)([^*\n]+?)\*(?!\*)""").replace(t) { it.groupValues[1] }
    t = Regex("""(?<!_)_(?!\s)([^_\n]+?)_(?!_)""").replace(t) { it.groupValues[1] }
    // Código `x` → x
    t = Regex("""`([^`]+)`""").replace(t) { it.groupValues[1] }
    // Títulos "# " / "## " no início da linha
    t = Regex("""(?m)^\s{0,3}#{1,6}\s+""").replace(t, "")
    // Referências de citação tipo 【2†L1-L4】 (que alguns modelos inserem)
    t = Regex("""【[^】]*】""").replace(t, "")
    return t.trim()
}
