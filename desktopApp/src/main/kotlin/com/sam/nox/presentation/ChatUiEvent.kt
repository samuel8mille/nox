package com.sam.nox.presentation

import com.sam.nox.chat.ConfirmDecision
import com.sam.nox.ui.orb.NoxMood

/** Eventos iniciados pela UI na tela de chat. */
sealed interface ChatUiEvent {
    data class DraftChanged(val value: String) : ChatUiEvent

    data object Send : ChatUiEvent

    data object Reset : ChatUiEvent

    data object ToggleMic : ChatUiEvent

    data object Poke : ChatUiEvent

    data object HistoryPrevious : ChatUiEvent

    data object HistoryNext : ChatUiEvent

    data class SetMoodOverride(val mood: NoxMood?) : ChatUiEvent

    data class ResolveConfirmation(val decision: ConfirmDecision) : ChatUiEvent
}
