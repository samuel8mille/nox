package com.sam.nox

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

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
