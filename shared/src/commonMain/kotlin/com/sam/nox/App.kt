package com.sam.nox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.ui.orb.NoxOrb
import com.sam.nox.ui.theme.NoxTheme

/**
 * Entrada comum (Android/iOS/preview). Mostra o orbe da Nox.
 * No Desktop, a janela de chat completa é montada em `desktopApp`.
 */
@Composable
@Preview
fun App() {
    NoxTheme {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(NoxTheme.colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            NoxOrb(mood = NoxMood.Idle, size = 240.dp)
        }
    }
}
