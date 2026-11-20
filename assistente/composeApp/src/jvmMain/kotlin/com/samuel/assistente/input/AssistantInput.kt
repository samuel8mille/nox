package com.samuel.assistente.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Eventos vindos de fora da janela de chat. Hoje só a UI usa o chat, mas o comando de voz,
 * o atalho global e o ícone na barra de menus vão publicar aqui, sem conhecer a UI.
 */
sealed interface AssistantEvent {
    /** Texto a enviar como se o usuário tivesse digitado (ex.: transcrição de voz). */
    data class Prompt(val text: String) : AssistantEvent

    /** Pedido para mostrar e focar a janela (ex.: atalho global, clique no ícone da barra). */
    data object ShowWindow : AssistantEvent
}

class AssistantEventBus {
    private val _events = MutableSharedFlow<AssistantEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AssistantEvent> = _events.asSharedFlow()

    fun emit(event: AssistantEvent) {
        _events.tryEmit(event)
    }
}

/** Contrato para as próximas fontes de entrada (voz, atalho global, ...). */
interface InputSource : AutoCloseable {
    fun start(bus: AssistantEventBus)
}
