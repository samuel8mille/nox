package com.sam.nox.ui.orb

import androidx.compose.ui.graphics.Color

/**
 * Estado expressivo do orbe da Nox. O ViewModel expõe este valor; o orbe só desenha.
 *
 * Mapeamento típico: campo vazio e mic desligado → [Idle]; texto no campo ou mic
 * ligado → [Listening]; requisição em andamento → [Thinking]; resposta chegou →
 * [Speaking] (por alguns segundos) → volta a [Idle]/[Listening]; falha → [Error].
 */
enum class NoxMood {
    Idle,
    Listening,
    Thinking,
    Speaking,
    Error,
}

/**
 * Paleta e parâmetros de animação de cada estado, extraídos do design v2
 * (OrbStates.dc.html). Tempos em ms; dimensões em fração do `size` do orbe.
 */
internal data class MoodSpec(
    val c1: Color,
    val c2: Color,
    val c3: Color,
    /** Período da "respiração"/pulso do corpo, em ms. */
    val bodyMillis: Int,
    /** Período do giro do redemoinho interno, em ms. */
    val swirlMillis: Int,
    /** Opacidade do redemoinho. */
    val swirlAlpha: Float,
    /** Alpha base do glow externo. */
    val glowAlpha: Float,
    /** Altura dos olhos (fração do size). */
    val eyeHeight: Float,
    /** Largura dos olhos (fração do size). */
    val eyeWidth: Float = 0.07f,
    /** Opacidade do anel de marcações (ticks); 0 = invisível. */
    val ticksAlpha: Float = 0f,
    /** Sentido dos ticks: +1 horário, -1 anti-horário. */
    val ticksDir: Int = 1,
    /** Período dos ticks em ms. */
    val ticksMillis: Int = 30000,
    /** Escala do grupo de partículas. */
    val particleScale: Float = 1f,
    /** Alpha do grupo de partículas. */
    val particleAlpha: Float = 0.75f,
    /** Anéis: 0 nenhum, 1 ripple (Listening), 2 wave (Speaking). */
    val ringMode: Int = 0,
    /** Satélite + rastro visíveis (Thinking). */
    val satellite: Boolean = false,
    /** Halo de 3 anéis: 0 nenhum, 1 líquido (Listening/Speaking), 2 giroscópio (Thinking). */
    val haloMode: Int = 0,
    /** Intensidade do halo (opacidade/velocidade). */
    val haloIntensity: Float = 1f,
    /** Tipo de boca a desenhar. */
    val mouth: MouthKind,
) {
    val c1Color get() = c1
}

/** Forma da boca por estado. */
internal enum class MouthKind { Smile, O, Hmm, Speak, Sad, Poke }

internal fun specFor(mood: NoxMood): MoodSpec = when (mood) {
    NoxMood.Idle -> MoodSpec(
        c1 = Color(0xFFA99BFF), c2 = Color(0xFF5B6CF0), c3 = Color(0xFF1B1550),
        bodyMillis = 4000, swirlMillis = 10000, swirlAlpha = 0.65f, glowAlpha = 0.5f,
        eyeHeight = 0.15f, mouth = MouthKind.Smile,
    )
    NoxMood.Listening -> MoodSpec(
        c1 = Color(0xFF7FE3F5), c2 = Color(0xFF4F8DF5), c3 = Color(0xFF0E2552),
        bodyMillis = 2200, swirlMillis = 5000, swirlAlpha = 0.65f, glowAlpha = 0.75f,
        eyeHeight = 0.18f, ticksAlpha = 0.55f,
        particleScale = 0.82f, particleAlpha = 1f, ringMode = 1,
        haloMode = 1, haloIntensity = 0.55f,
        mouth = MouthKind.O,
    )
    NoxMood.Thinking -> MoodSpec(
        c1 = Color(0xFFC29BFF), c2 = Color(0xFF7A5CF0), c3 = Color(0xFF24104F),
        bodyMillis = 4000, swirlMillis = 2400, swirlAlpha = 0.85f, glowAlpha = 0.5f,
        eyeHeight = 0.08f, ticksAlpha = 0.3f, ticksDir = -1, ticksMillis = 6000,
        particleAlpha = 1f, haloMode = 2, haloIntensity = 0.7f,
        mouth = MouthKind.Hmm,
    )
    NoxMood.Speaking -> MoodSpec(
        c1 = Color(0xFF8CF0D2), c2 = Color(0xFF4FB9E8), c3 = Color(0xFF0D2E45),
        bodyMillis = 300, swirlMillis = 4000, swirlAlpha = 0.65f, glowAlpha = 0.85f,
        eyeHeight = 0.06f, particleScale = 1.12f, particleAlpha = 1f, ringMode = 2,
        haloMode = 1, haloIntensity = 0.95f,
        mouth = MouthKind.Speak,
    )
    NoxMood.Error -> MoodSpec(
        c1 = Color(0xFFFFB37A), c2 = Color(0xFFE8604F), c3 = Color(0xFF4A1414),
        bodyMillis = 2400, swirlMillis = 18000, swirlAlpha = 0.4f, glowAlpha = 0.3f,
        eyeHeight = 0.025f, eyeWidth = 0.1f,
        particleScale = 0.95f, particleAlpha = 0.25f,
        mouth = MouthKind.Sad,
    )
}
