package com.sam.nox.presentation

import com.sam.nox.chat.ConfirmDecision
import com.sam.nox.ui.orb.NoxMood

/** Eventos iniciados pela UI na tela de chat. */
sealed interface ChatUiEvent {
    /** O rascunho mudou (digitação ou injeção programática). */
    data class DraftChanged(val value: String) : ChatUiEvent

    /** Enviar o rascunho atual. */
    data object Send : ChatUiEvent

    /** Começar uma conversa nova. */
    data object Reset : ChatUiEvent

    /** Ligar/desligar o microfone. */
    data object ToggleMic : ChatUiEvent

    /** Cutucar o orbe (clique). */
    data object Poke : ChatUiEvent

    /** Navegar para a mensagem anterior no histórico (seta para cima). */
    data object HistoryPrevious : ChatUiEvent

    /** Navegar para a mensagem seguinte no histórico (seta para baixo). */
    data object HistoryNext : ChatUiEvent

    /** Debug: forçar um estado do orbe (ou null para automático). */
    data class SetMoodOverride(val mood: NoxMood?) : ChatUiEvent

    /** Resolver a confirmação pendente com a decisão do usuário. */
    data class ResolveConfirmation(val decision: ConfirmDecision) : ChatUiEvent
}
