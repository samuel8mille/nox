package com.samuel.assistente.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.samuel.assistente.presentation.chat.VoiceState

@Composable
fun InputBar(
    value: String,
    canSend: Boolean,
    voice: VoiceState,
    canUseMic: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val isListening = voice == VoiceState.Listening

    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        if (voice is VoiceState.Preparing) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    voice.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = isListening, // durante a fala o campo mostra a transcrição parcial
                placeholder = { Text(if (isListening) "Ouvindo…" else "Escreva ou clique no microfone…") },
                maxLines = 6,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .focusRequester(focusRequester)
                    // Enter envia; Shift+Enter segue o comportamento padrão (nova linha).
                    .onPreviewKeyEvent { event ->
                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (!isEnter || event.isShiftPressed) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyDown && canSend) onSend()
                        true // consome KeyDown e KeyUp para o Enter não inserir "\n"
                    },
            )
            MicButton(isListening = isListening, enabled = canUseMic || isListening, onClick = onMicClick)
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
private fun MicButton(isListening: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (isListening) {
        val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "pulse",
        )
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).alpha(pulse),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(AppIcons.Stop, contentDescription = "Parar de ouvir e enviar")
        }
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(AppIcons.Mic, contentDescription = "Perguntar por voz")
        }
    }
}
