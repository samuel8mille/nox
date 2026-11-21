package com.sam.nox.ui

import androidx.compose.ui.graphics.Color
import com.sam.nox.ui.orb.NoxMood

/** Rótulo, dica e cor do ponto de status para cada estado do orbe. */
internal data class MoodStatus(val label: String, val hint: String, val dot: Color)

internal fun statusFor(mood: NoxMood): MoodStatus = when (mood) {
    NoxMood.Idle -> MoodStatus("Pronto", "Pergunte qualquer coisa", Color(0xFFA99BFF))
    NoxMood.Listening -> MoodStatus("Ouvindo", "Acompanhando o que você digita", Color(0xFF7FE3F5))
    NoxMood.Thinking -> MoodStatus("Pensando", "Consultando o modelo", Color(0xFFC29BFF))
    NoxMood.Speaking -> MoodStatus("Respondendo", "Resposta recebida", Color(0xFF8CF0D2))
    NoxMood.Error -> MoodStatus("Algo deu errado", "Veja os detalhes na conversa", Color(0xFFFFB37A))
}
