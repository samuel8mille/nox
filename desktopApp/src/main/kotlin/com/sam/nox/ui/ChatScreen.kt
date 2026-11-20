package com.sam.nox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.nox.presentation.ChatUiState
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.ui.theme.NoxTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Janela de chat da Nox: title bar, painel do orbe à esquerda e conversa à direita.
 * O rosto do orbe segue o cursor (olhar normalizado em relação ao centro da janela).
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onToggleMic: () -> Unit,
    onPoke: () -> Unit,
    onSetMoodOverride: (NoxMood?) -> Unit,
    onApproveAction: () -> Unit,
    onRejectAction: () -> Unit,
    onHistoryPrevious: () -> Unit,
    onHistoryNext: () -> Unit,
) {
    val colors = NoxTheme.colors
    var size by remember { mutableStateOf(Offset.Zero) } // largura/altura da janela em px
    var look by remember { mutableStateOf(Offset.Zero) } // deslocamento do rosto em dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .onGloballyPositioned { size = Offset(it.size.width.toFloat(), it.size.height.toFloat()) }
            .onPointerEvent(PointerEventType.Move) { e ->
                val pos = e.changes.first().position
                if (size.x > 0f && size.y > 0f) {
                    val nx = (pos.x - size.x / 2f) / (size.x / 2f)
                    val ny = (pos.y - size.y / 2f) / (size.y / 2f)
                    look = Offset(
                        x = max(-1f, min(1f, nx)) * 14f,
                        y = max(-1f, min(1f, ny)) * 10f,
                    )
                }
            }
            .onPointerEvent(PointerEventType.Exit) { look = Offset.Zero },
    ) {
        TitleBar(
            moodOverride = state.moodOverride,
            onSetMoodOverride = onSetMoodOverride,
        )
        Row(modifier = Modifier.fillMaxSize()) {
            OrbPanel(
                mood = state.effectiveMood,
                look = look,
                poked = state.poked,
                onPoke = onPoke,
                statusHint = state.statusHint,
                modifier = Modifier.widthIn(min = 340.dp),
            )
            VerticalLine()
            ChatPanel(
                state = state,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onReset = onReset,
                onToggleMic = onToggleMic,
                onHistoryPrevious = onHistoryPrevious,
                onHistoryNext = onHistoryNext,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Diálogo de confirmação para ações que alteram algo externo (GitHub, etc.).
    state.pendingConfirmation?.let { preview ->
        ConfirmActionDialog(
            preview = preview,
            onApprove = onApproveAction,
            onReject = onRejectAction,
        )
    }
}

/** Overlay modal pedindo aprovação de uma ação sensível antes de executá-la. */
@Composable
private fun ConfirmActionDialog(
    preview: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val colors = NoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Text("Confirmar ação", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                preview,
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .clickable(onClick = onReject)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) { Text("Recusar", color = colors.text, fontSize = 14.sp) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.user)
                        .clickable(onClick = onApprove)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) { Text("Aprovar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun TitleBar(
    moodOverride: NoxMood?,
    onSetMoodOverride: (NoxMood?) -> Unit,
) {
    val colors = NoxTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.panel),
        contentAlignment = Alignment.Center,
    ) {
        Text("Nox", color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
        ) {
            DebugMenu(moodOverride = moodOverride, onSetMoodOverride = onSetMoodOverride)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

/** Menu de debug na barra: Debug → Estados → (força cada NoxMood ou automático). */
@Composable
private fun DebugMenu(
    moodOverride: NoxMood?,
    onSetMoodOverride: (NoxMood?) -> Unit,
) {
    val colors = NoxTheme.colors
    var open by remember { mutableStateOf(false) }
    var statesOpen by remember { mutableStateOf(false) }

    val active = moodOverride != null
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) colors.user.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (active) colors.user else colors.line, RoundedCornerShape(8.dp))
            .clickable { open = true }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        val label = if (active) "Debug · ${moodOverride!!.name}" else "Debug"
        Text(label, color = if (active) colors.text else colors.muted, fontSize = 12.sp)
    }

    DropdownMenu(
        expanded = open,
        onDismissRequest = { open = false; statesOpen = false },
    ) {
        // Submenu "Estados"
        DropdownMenuItem(
            text = { Text("Estados  ▸") },
            onClick = { statesOpen = !statesOpen },
        )
        if (statesOpen) {
            NoxMood.entries.forEach { mood ->
                val selected = moodOverride == mood
                DropdownMenuItem(
                    text = {
                        Text(
                            (if (selected) "• " else "   ") + mood.name,
                            color = if (selected) colors.accent else colors.text,
                        )
                    },
                    onClick = {
                        onSetMoodOverride(mood)
                        open = false; statesOpen = false
                    },
                )
            }
        }
        Divider(color = colors.line)
        DropdownMenuItem(
            text = { Text("Automático (desligar debug)") },
            onClick = {
                onSetMoodOverride(null)
                open = false; statesOpen = false
            },
        )
    }
}

@Composable
private fun VerticalLine() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(NoxTheme.colors.line))
}
