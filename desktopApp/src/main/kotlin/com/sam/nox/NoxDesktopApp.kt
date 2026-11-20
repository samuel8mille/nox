package com.sam.nox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.sam.nox.data.ChatRepository
import com.sam.nox.presentation.ChatViewModel
import com.sam.nox.ui.ChatScreen
import com.sam.nox.ui.theme.NoxTheme
import java.nio.file.Files
import java.nio.file.Path

/**
 * App Desktop da Nox: monta data → presentation → ui e exibe a janela de chat.
 * O microfone liga-se ao Whisper quando há modelo GGML disponível.
 *
 * @param model id do modelo no Groq (ex.: "openai/gpt-oss-120b").
 */
@Composable
fun NoxDesktopApp(model: String = "openai/gpt-oss-120b") {
    val scope = rememberCoroutineScope()
    val voice = remember { createVoiceInput(resolveWhisperModel()) }
    // O ViewModel cria o repository passando seu próprio confirm (para pausar
    // ações que precisam de aprovação do usuário).
    val viewModel = remember {
        ChatViewModel(
            scope = scope,
            repositoryFactory = { confirm -> ChatRepository(modelName = model, confirm = confirm) },
            voice = voice,
        )
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.close() }
    }

    val state by viewModel.uiState.collectAsState()

    NoxTheme(dark = true) {
        ChatScreen(
            state = state,
            onDraftChange = viewModel::onDraftChange,
            onSend = viewModel::send,
            onReset = viewModel::reset,
            onToggleMic = viewModel::toggleMic,
            onPoke = viewModel::poke,
            onSetMoodOverride = viewModel::setMoodOverride,
            onApproveAction = viewModel::approveAction,
            onRejectAction = viewModel::rejectAction,
            onHistoryPrevious = viewModel::historyPrevious,
            onHistoryNext = viewModel::historyNext,
        )
    }
}

/**
 * Procura o modelo GGML do Whisper em locais conhecidos. Prioriza a propriedade
 * de sistema `nox.model` e a variável `NOX_MODEL`.
 */
fun resolveWhisperModel(): Path? {
    val candidates = listOfNotNull(
        System.getProperty("nox.model"),
        System.getenv("NOX_MODEL"),
        "ggml-base.bin",
        "models/ggml-base.bin",
        System.getProperty("user.dir")?.let { "$it/ggml-base.bin" },
    )
    return candidates
        .map { Path.of(it) }
        .firstOrNull { Files.exists(it) }
}
