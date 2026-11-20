package com.samuel.assistente.presentation.chat

import java.util.UUID

enum class Author { USER, ASSISTANT, ERROR }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val author: Author,
    val text: String,
)

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Preparing(val message: String) : VoiceState
    data object Listening : VoiceState
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isTyping: Boolean = false,
    val voice: VoiceState = VoiceState.Idle,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isTyping && voice == VoiceState.Idle
    val canUseMic: Boolean get() = !isTyping && voice !is VoiceState.Preparing
}
