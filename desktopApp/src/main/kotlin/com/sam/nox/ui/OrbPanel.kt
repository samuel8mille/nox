package com.sam.nox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.ui.orb.NoxOrb
import com.sam.nox.ui.theme.NoxTheme

/**
 * Painel esquerdo: orbe grande (clicável → cutucada), nome "Nox", pill de
 * status, dica e rodapé em `muted` com a nota de "em breve".
 */
@Composable
fun OrbPanel(
    mood: NoxMood,
    look: Offset,
    poked: Boolean,
    onPoke: () -> Unit,
    statusHint: String,
    modifier: Modifier = Modifier,
) {
    val colors = NoxTheme.colors
    val status = statusFor(mood)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.panel)
            .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterVertically),
        ) {
            // Área clicável de 320×340 em torno do orbe de 240dp.
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(width = 320.dp, height = 340.dp)
                    .clickable(interactionSource = interaction, indication = null, onClick = onPoke),
                contentAlignment = Alignment.Center,
            ) {
                NoxOrb(mood = mood, size = 240.dp, look = look, poked = poked)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Nox",
                    color = colors.text,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusPill(label = status.label, dot = status.dot)
                Text(
                    text = statusHint,
                    color = colors.muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "Atalho ⌥ Espaço e ícone na barra de menus chegam em breve",
            color = colors.muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

@Composable
private fun StatusPill(label: String, dot: Color) {
    val colors = NoxTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.line, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(text = label, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
