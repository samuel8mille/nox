package com.samuel.assistente.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samuel.assistente.presentation.chat.Author
import com.samuel.assistente.presentation.chat.ChatMessage

private val UserShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
private val AssistantShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)

@Composable
fun MessageBubble(message: ChatMessage) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (message.author) {
        Author.USER -> colors.primary to colors.onPrimary
        Author.ASSISTANT -> colors.surfaceVariant to colors.onSurfaceVariant
        Author.ERROR -> colors.errorContainer to colors.onErrorContainer
    }
    val isUser = message.author == Author.USER

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            color = container,
            contentColor = content,
            shape = if (isUser) UserShape else AssistantShape,
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            SelectionContainer {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** "digitando..." com três pontos pulsando, alinhado à esquerda como uma resposta. */
@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = AssistantShape) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "digitando",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 500, delayMillis = index * 160),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "dot$index",
                    )
                    Box(
                        Modifier
                            .size(6.dp)
                            .alpha(alpha)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                }
            }
        }
    }
}
