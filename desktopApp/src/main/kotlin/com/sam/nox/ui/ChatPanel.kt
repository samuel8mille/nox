package com.sam.nox.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sam.nox.presentation.Author
import com.sam.nox.presentation.ChatUiState
import com.sam.nox.presentation.Message
import com.sam.nox.ui.orb.NoxMood
import com.sam.nox.ui.orb.NoxOrb
import com.sam.nox.ui.theme.NoxTheme

/**
 * Painel direito: cabeçalho da conversa, lista rolável de mensagens (com
 * indicador "digitando") e o compositor no rodapé.
 */
@Composable
fun ChatPanel(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onToggleMic: () -> Unit,
    onHistoryPrevious: () -> Unit,
    onHistoryNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NoxTheme.colors
    Column(modifier = modifier.fillMaxHeight().background(colors.bg)) {
        ChatHeader(modelName = state.modelName, onReset = onReset)
        MessageList(
            state = state,
            modifier = Modifier.weight(1f),
        )
        Composer(
            draft = state.draft,
            placeholder = state.placeholder,
            sendEnabled = state.sendEnabled,
            micOn = state.micOn,
            micEnabled = state.micEnabled,
            micAvailable = state.micAvailable,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onToggleMic = onToggleMic,
            onHistoryPrevious = onHistoryPrevious,
            onHistoryNext = onHistoryNext,
        )
    }
}

@Composable
private fun ChatHeader(modelName: String, onReset: () -> Unit) {
    val colors = NoxTheme.colors
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Conversa", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "$modelName · histórico completo enviado a cada mensagem",
                color = colors.muted,
                fontSize = 12.sp,
            )
        }
        GhostButton(text = "Nova conversa", onClick = onReset)
    }
    BottomLine()
}

@Composable
private fun BottomLine() {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .size(1.dp)
            .background(NoxTheme.colors.line),
    )
}

@Composable
private fun MessageList(state: ChatUiState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + if (state.isSending) 1 else 0

    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding =
        androidx.compose.foundation.layout
            .PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.messages, key = { it.id }) { msg -> MessageBubble(msg) }
        if (state.isSending) {
            item("typing") { TypingIndicator() }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val colors = NoxTheme.colors
    when (message.author) {
        Author.User ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier =
                    Modifier
                        .widthIn(max = 460.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                        .background(colors.user)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(message.text, color = Color.White, fontSize = 15.sp)
                }
            }

        Author.Nox ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom,
            ) {
                NoxOrb(mood = NoxMood.Idle, size = 28.dp, compact = true)
                Box(
                    modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .widthIn(max = 500.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(message.text, color = colors.text, fontSize = 15.sp)
                }
            }

        Author.Error ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom,
            ) {
                NoxOrb(mood = NoxMood.Error, size = 28.dp, compact = true)
                Column(
                    modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .widthIn(max = 500.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .background(colors.dangerBg)
                        .border(1.dp, colors.danger, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Não consegui responder",
                        color = colors.danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(message.text, color = colors.text, fontSize = 15.sp)
                }
            }
    }
}

@Composable
private fun TypingIndicator() {
    val colors = NoxTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        NoxOrb(mood = NoxMood.Thinking, size = 28.dp, compact = true)
        Row(
            modifier =
            Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i -> TypingDot(delayMillis = i * 150) }
        }
    }
}

@Composable
private fun TypingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "dot")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
        infiniteRepeatable(
            animation = tween(1200, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotT",
    )
    val colors = NoxTheme.colors
    Box(
        modifier =
        Modifier
            .offset(y = (-3 * t).dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(colors.muted.copy(alpha = 0.3f + 0.7f * t)),
    )
}

@Composable
private fun Composer(
    draft: String,
    placeholder: String,
    sendEnabled: Boolean,
    micOn: Boolean,
    micEnabled: Boolean,
    micAvailable: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleMic: () -> Unit,
    onHistoryPrevious: () -> Unit,
    onHistoryNext: () -> Unit,
) {
    val colors = NoxTheme.colors
    val borderColor = colors.line

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp, max = 150.dp)
                    .padding(vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val singleLine = !draft.contains('\n')
                        when {
                            event.key == Key.Enter && !event.isShiftPressed -> {
                                if (sendEnabled) onSend()
                                true
                            }
                            // Setas navegam o histórico quando o texto é de uma linha.
                            event.key == Key.DirectionUp && singleLine -> {
                                onHistoryPrevious()
                                true
                            }
                            event.key == Key.DirectionDown && singleLine -> {
                                onHistoryNext()
                                true
                            }
                            else -> false
                        }
                    },
                textStyle = TextStyle(color = colors.text, fontSize = 15.sp, lineHeight = 22.sp),
                cursorBrush =
                androidx.compose.ui.graphics
                    .SolidColor(colors.accent),
                maxLines = 6,
                decorationBox = { inner ->
                    // Placeholder e campo ocupam a mesma caixa, alinhados verticalmente,
                    // para o cursor ficar na mesma linha do texto/placeholder.
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (draft.isEmpty()) {
                            Text(placeholder, color = colors.muted, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
            if (micAvailable) {
                MicButton(on = micOn, enabled = micEnabled, onClick = onToggleMic)
            }
            SendButton(enabled = sendEnabled, onClick = onSend)
        }
        Text(
            text = "Enter envia · Shift + Enter quebra linha",
            color = colors.muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MicButton(on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = NoxTheme.colors

    // Anel pulsante quando ligado (0→10dp, some em 1,6s).
    val ring by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "micRing",
    )

    val bg = if (on) colors.micBg else Color.Transparent
    val tint = if (on) colors.mic else colors.muted

    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (on) {
            Box(
                modifier =
                Modifier
                    .size((40 + ring * 10).dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.micBg.copy(alpha = (1f - ring) * 0.6f)),
            )
        }
        Box(
            modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .then(if (enabled || on) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            MicIcon(tint = tint, size = 20.dp)
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = NoxTheme.colors
    Box(
        modifier =
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.user.copy(alpha = if (enabled) 1f else 0.35f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        SendArrowIcon(tint = Color.White, size = 18.dp)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    val colors = NoxTheme.colors
    Box(
        modifier =
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
