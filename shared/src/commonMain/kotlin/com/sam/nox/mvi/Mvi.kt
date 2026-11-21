package com.sam.nox.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Framework MVI mínimo e reutilizável para as ViewModels do Nox, inspirado no
 * `core:mvi` do projeto Recorda e adaptado ao Kotlin Multiplatform (sem `@StringRes`
 * do Android — textos são `String`).
 *
 * Uma ViewModel compõe os três papéis por **delegação** (`by`):
 *
 * ```
 * class ChatViewModel(...) :
 *     UiState<ChatUiState> by UiStateImpl(ChatUiState()),
 *     UiEvent<ChatUiEvent> by UiEventImpl(),
 *     UiEffect<ChatUiEffect> by UiEffectImpl()
 * ```
 *
 * Assim ela ganha, sem reimplementar: `stateFlow`/`setState`/`showLoading`/`showError`
 * (estado), `eventFlow`/`sendEvent` (entrada da UI) e `effectFlow`/`sendEffect`
 * (efeitos de uma vez para a UI).
 */

/** Contrato do estado de conteúdo de uma tela. */
interface ScreenUiState {
    /** Título da tela (texto já localizado). */
    val title: String
}

/** Overlay de carregamento sobre o conteúdo. */
data class LoadingUiState(val message: String = "")

/** Overlay de erro sobre o conteúdo. */
data class ErrorUiState(val message: String)

/** Envolve o estado de [content] com overlays opcionais de [loading] e [error]. */
data class ProcessUiState<T : ScreenUiState>(
    val content: T,
    val loading: LoadingUiState? = null,
    val error: ErrorUiState? = null,
)

/** Gerencia o [ProcessUiState] de uma tela, exposto como [StateFlow]. */
interface UiState<T : ScreenUiState> {
    val stateFlow: StateFlow<ProcessUiState<T>>
    fun setState(block: ProcessUiState<T>.() -> ProcessUiState<T>)
    fun showLoading(loading: LoadingUiState)
    fun hideLoading()
    fun showError(error: ErrorUiState)
    fun hideError()
}

/** Implementação padrão de [UiState] respaldada por um [MutableStateFlow]. */
class UiStateImpl<T : ScreenUiState>(value: T) : UiState<T> {
    private val _stateFlow = MutableStateFlow(ProcessUiState(value))
    override val stateFlow: StateFlow<ProcessUiState<T>> = _stateFlow

    override fun setState(block: ProcessUiState<T>.() -> ProcessUiState<T>) {
        _stateFlow.value = _stateFlow.value.block()
    }

    override fun showLoading(loading: LoadingUiState) = setState { copy(loading = loading) }
    override fun hideLoading() = setState { copy(loading = null) }
    override fun showError(error: ErrorUiState) = setState { copy(error = error) }
    override fun hideError() = setState { copy(error = null) }
}

/** Entrega eventos da UI para a ViewModel via [SharedFlow]. */
interface UiEvent<T> {
    val eventFlow: SharedFlow<T>
    suspend fun sendEvent(event: T)
}

/** Implementação padrão de [UiEvent] respaldada por um [MutableSharedFlow]. */
class UiEventImpl<T> : UiEvent<T> {
    private val _eventFlow = MutableSharedFlow<T>()
    override val eventFlow: SharedFlow<T> = _eventFlow

    override suspend fun sendEvent(event: T) {
        _eventFlow.emit(event)
    }
}

/** Emite efeitos colaterais de uma vez da ViewModel para a UI via [SharedFlow]. */
interface UiEffect<T> {
    val effectFlow: SharedFlow<T>
    suspend fun sendEffect(effect: T)
}

/** Implementação padrão de [UiEffect] respaldada por um [MutableSharedFlow]. */
class UiEffectImpl<T> : UiEffect<T> {
    private val _effectFlow = MutableSharedFlow<T>(extraBufferCapacity = 1)
    override val effectFlow: SharedFlow<T> = _effectFlow

    override suspend fun sendEffect(effect: T) {
        _effectFlow.emit(effect)
    }
}
