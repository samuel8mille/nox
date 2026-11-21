package com.sam.nox.debug

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Janela só com a galeria de estados do orbe (inspeção visual). */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Nox — estados do orbe",
        state = rememberWindowState(width = 1280.dp, height = 560.dp),
    ) {
        OrbGallery()
    }
}
