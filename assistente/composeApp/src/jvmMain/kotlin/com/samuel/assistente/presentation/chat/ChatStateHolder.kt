package com.samuel.assistente.presentation.chat

import com.samuel.assistente.data.ChatError
import com.samuel.assistente.data.ChatRepository
import com.samuel.assistente.input.AssistantEvent
import com.samuel.assistente.input.AssistantEventBus
import com.samuel.assistente.input.voice.SpeechError
import com.samuel.assistente.input.voice.SpeechEvent
import com.samuel.assistente.input.voice.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Equivalente desktop de um ViewModel: expõe [state] como StateFlow e roda as chamadas em
 * corrotinas. O escopo usa Dispatchers.Main (thread do Swing); a rede roda no pool do Ktor e
 * a gravação de áudio em Dispatchers.IO, então nada bloqueia a UI.
 */
class ChatStateHolder(
    private val repository: ChatRepository,
    private val speechRecognizer: SpeechRecognizer,
    eventBus: AssistantEventBus,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : AutoCloseable {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var voiceJob: Job? = null

    init {
        scope.launch {
            eventBus.events
                .filterIsInstance<AssistantEvent.Prompt>()
                .collect { send(it.text) }
        }
    }

    fun onInputChange(value: String) = _state.update { it.copy(input = value) }

    fun onSendClick() {
        if (send(_state.value.input)) _state.update { it.copy(input = "") }
    }

    /** 1º clique começa a ouvir; 2º clique encerra e envia o que foi dito. */
    fun onMicClick() {
        val current = _state.value
        when {
            current.voice == VoiceState.Listening -> speechRecognizer.stopListening()
            current.canUseMic -> startListening()
        }
    }

    private fun startListening() {
        voiceJob?.cancel()
        _state.update { it.copy(input = "") }
        voiceJob = scope.launch {
            try {
                speechRecognizer.listen().collect { event ->
                    when (event) {
                        is SpeechEvent.Preparing ->
                            _state.update { it.copy(voice = VoiceState.Preparing(event.message)) }
                        SpeechEvent.Listening ->
                            _state.update { it.copy(voice = VoiceState.Listening) }
                        is SpeechEvent.Partial ->
                            _state.update { it.copy(input = event.text) }
                        is SpeechEvent.Final -> {
                            _state.update { it.copy(voice = VoiceState.Idle, input = event.text) }
                            if (event.text.isNotBlank()) onSendClick()
                        }
                    }
                }
            } catch (e: SpeechError) {
                addError(e.message.orEmpty())
            } finally {
                _state.update { it.copy(voice = VoiceState.Idle) }
            }
        }
    }

    /** Retorna false se não enviou (texto vazio, resposta em andamento ou microfone ativo). */
    private fun send(raw: String): Boolean {
        val text = raw.trim()
        val current = _state.value
        if (text.isEmpty() || current.isTyping || current.voice != VoiceState.Idle) return false

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(author = Author.USER, text = text),
                isTyping = true,
            )
        }

        scope.launch {
            val reply = try {
                ChatMessage(author = Author.ASSISTANT, text = repository.send(text))
            } catch (e: ChatError) {
                ChatMessage(author = Author.ERROR, text = e.message.orEmpty())
            }
            _state.update { it.copy(messages = it.messages + reply, isTyping = false) }
        }
        return true
    }

    private fun addError(text: String) = _state.update {
        it.copy(messages = it.messages + ChatMessage(author = Author.ERROR, text = text))
    }

    fun clearConversation() {
        if (_state.value.isTyping) return
        repository.clear()
        _state.update { ChatUiState(input = it.input) }
    }

    override fun close() {
        speechRecognizer.stopListening()
        scope.cancel()
    }
}
