package com.sam.nox.presentation

import com.sam.nox.mvi.ScreenUiState
import com.sam.nox.ui.orb.NoxMood

/**
 * Estado de conteúdo da tela de chat.
 *
 * @property micOn Microfone capturando agora.
 * @property micAvailable Há entrada de voz disponível (modelo Whisper carregado).
 * @property poked True por ~700ms após o clique no orbe (cutucada).
 * @property moodOverride Quando != null, força o estado do orbe (modo debug).
 * @property pendingConfirmation Preview da ação aguardando confirmação do usuário, ou null.
 */
data class ChatUiState(
    override val title: String = "Conversa",
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val mood: NoxMood = NoxMood.Idle,
    val isSending: Boolean = false,
    val modelName: String = "",
    val micOn: Boolean = false,
    val micAvailable: Boolean = false,
    val poked: Boolean = false,
    val moodOverride: NoxMood? = null,
    val pendingConfirmation: String? = null,
) : ScreenUiState {
    /** Estado efetivo que o orbe desenha: o override do debug ou o derivado. */
    val effectiveMood: NoxMood get() = moodOverride ?: mood

    /** Enviar desabilitado quando vazio, ou durante requisição/transcrição. */
    val sendEnabled: Boolean get() = draft.isNotBlank() && !isSending

    /** Microfone bloqueado durante Thinking/Speaking. */
    val micEnabled: Boolean get() = micAvailable && !isSending && mood != NoxMood.Speaking

    val placeholder: String get() = if (micOn) "Ouvindo… fale ou digite" else "Fale com o Nox…"
    val statusHint: String get() =
        when {
            moodOverride != null -> "Debug: estado forçado"
            micOn -> "Ouvindo pelo microfone"
            else ->
                when (mood) {
                    NoxMood.Idle -> "Pergunte qualquer coisa"
                    NoxMood.Listening -> "Acompanhando o que você digita"
                    NoxMood.Thinking -> "Consultando o modelo"
                    NoxMood.Speaking -> "Resposta recebida"
                    NoxMood.Error -> "Veja os detalhes na conversa"
                }
        }
}
