package com.samuel.assistente.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samuel.assistente.presentation.chat.ChatStateHolder
import com.samuel.assistente.presentation.chat.ChatUiState

@Composable
fun ChatScreen(stateHolder: ChatStateHolder) {
    val state by stateHolder.state.collectAsState()
    ChatContent(
        state = state,
        onInputChange = stateHolder::onInputChange,
        onSend = stateHolder::onSendClick,
        onMicClick = stateHolder::onMicClick,
    )
}

@Composable
fun ChatContent(
    state: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val itemCount = state.messages.size + if (state.isTyping) 1 else 0

    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            if (state.messages.isEmpty() && !state.isTyping) {
                EmptyState(Modifier.weight(1f).fillMaxWidth())
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.messages, key = { it.id }) { MessageBubble(it) }
                    if (state.isTyping) item(key = "typing") { TypingIndicator() }
                }
            }
            HorizontalDivider()
            InputBar(
                value = state.input,
                canSend = state.canSend,
                voice = state.voice,
                canUseMic = state.canUseMic,
                onValueChange = onInputChange,
                onSend = onSend,
                onMicClick = onMicClick,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Como posso ajudar?", style = MaterialTheme.typography.titleLarge)
        Text(
            "Enter envia · Shift+Enter quebra a linha · microfone pergunta por voz",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
