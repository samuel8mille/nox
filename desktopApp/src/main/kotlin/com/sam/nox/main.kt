package com.sam.nox

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Ponto de entrada da app Desktop: abre a janela principal do Nox. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nox",
        icon = painterResource("nox-icon.png"),
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        NoxDesktopApp()
    }
}
