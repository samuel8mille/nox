package com.sam.nox.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.ui.orb.NoxOrb
import com.sam.nox.ui.theme.NoxTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Galeria de inspeção: mostra os 5 estados do orbe lado a lado (como o
 * OrbStates.dc.html) para conferir cor, ritmo e expressão. Clicar num orbe
 * dispara a cutucada (poke) nele por 700ms.
 */
@Composable
fun OrbGallery() {
    NoxTheme(dark = true) {
        val colors = NoxTheme.colors
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Estados do orbe — clique num orbe para a cutucada",
                color = colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val items =
                    listOf(
                        NoxMood.Idle to "Pronto",
                        NoxMood.Listening to "Ouvindo",
                        NoxMood.Thinking to "Pensando",
                        NoxMood.Speaking to "Respondendo",
                        NoxMood.Error to "Erro",
                    )
                items.forEach { (mood, label) ->
                    GalleryCell(mood = mood, label = label, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun GalleryCell(mood: NoxMood, label: String, modifier: Modifier) {
    val colors = NoxTheme.colors
    var poked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier =
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.panel)
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        var look by remember { mutableStateOf(Offset.Zero) }
        var cellSize by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier =
            Modifier
                .size(200.dp)
                .onGloballyPositioned { cellSize = Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
                .onPointerEvent(PointerEventType.Move) { e ->
                    val pos = e.changes.first().position
                    if (cellSize.x > 0f) {
                        val nx = (pos.x - cellSize.x / 2f) / (cellSize.x / 2f)
                        val ny = (pos.y - cellSize.y / 2f) / (cellSize.y / 2f)
                        look = Offset(nx.coerceIn(-1f, 1f) * 14f, ny.coerceIn(-1f, 1f) * 10f)
                    }
                }.onPointerEvent(PointerEventType.Exit) { look = Offset.Zero }
                .clickable {
                    poked = true
                    scope.launch {
                        delay(700)
                        poked = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            NoxOrb(mood = mood, size = 160.dp, look = look, poked = poked)
        }
        Text(label, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(mood.name, color = colors.muted, fontSize = 12.sp)
    }
}
