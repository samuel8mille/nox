package com.samuel.assistente

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.samuel.assistente.di.AppContainer
import com.samuel.assistente.input.AssistantEvent
import com.samuel.assistente.presentation.chat.ChatStateHolder
import com.samuel.assistente.ui.chat.ChatScreen
import com.samuel.assistente.ui.theme.AppTheme
import kotlinx.coroutines.flow.filterIsInstance

fun main() = application {
    val container = remember { AppContainer().also { it.start() } }
    val chat = remember {
        ChatStateHolder(container.chatRepository, container.speechRecognizer, container.eventBus)
    }

    DisposableEffect(Unit) {
        onDispose {
            chat.close()
            container.close()
        }
    }

    // Visibilidade fica fora da janela para o futuro ícone na barra de menus (Tray)
    // e o atalho global poderem mostrar/esconder a janela.
    var isVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        container.eventBus.events
            .filterIsInstance<AssistantEvent.ShowWindow>()
            .collect { isVisible = true }
    }

    Window(
        onCloseRequest = ::exitApplication,
        visible = isVisible,
        title = "MinhaAssistente",
        state = rememberWindowState(size = DpSize(560.dp, 760.dp)),
    ) {
        AppTheme {
            ChatScreen(chat)
        }
    }
}
