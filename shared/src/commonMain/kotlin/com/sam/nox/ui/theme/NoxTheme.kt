package com.sam.nox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens de cor da Nox (ver design/NOX_UI_HANDOFF.md). São expostos via
 * [LocalNoxColors]; use `NoxTheme.colors` nos composables.
 *
 * @property user Bolha/botão do usuário.
 * @property mic Cor do ícone de microfone quando ativo.
 * @property micBg Fundo do botão de microfone ativo (e cor do anel pulsante).
 */
@Immutable
data class NoxColors(
    val bg: Color,
    val panel: Color,
    val surface: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val user: Color,
    val accent: Color,
    val danger: Color,
    val dangerBg: Color,
    val mic: Color,
    val micBg: Color,
    val isDark: Boolean,
)

val NoxDarkColors =
    NoxColors(
        bg = Color(0xFF0B0C12),
        panel = Color(0xFF10121A),
        surface = Color(0xFF181B27),
        line = Color.White.copy(alpha = 0.08f),
        text = Color(0xFFECEDF5),
        muted = Color(0xFF9A9EB4),
        user = Color(0xFF5B5BD6),
        accent = Color(0xFFA99BFF),
        danger = Color(0xFFFF8A80),
        dangerBg = Color(0xFFE8604F).copy(alpha = 0.12f),
        mic = Color(0xFF7FE3F5),
        micBg = Color(0xFF7FE3F5).copy(alpha = 0.16f),
        isDark = true,
    )

val NoxLightColors =
    NoxColors(
        bg = Color(0xFFF6F6FA),
        panel = Color(0xFFEEEEF5),
        surface = Color(0xFFFFFFFF),
        line = Color(0xFF0F111E).copy(alpha = 0.09f),
        text = Color(0xFF14151F),
        muted = Color(0xFF5A5E73),
        user = Color(0xFF4B4BC4),
        accent = Color(0xFF5B5BD6),
        danger = Color(0xFFB42318),
        dangerBg = Color(0xFFE8604F).copy(alpha = 0.10f),
        mic = Color(0xFF0E7490),
        micBg = Color(0xFF0E7490).copy(alpha = 0.12f),
        isDark = false,
    )

val LocalNoxColors: ProvidableCompositionLocal<NoxColors> =
    staticCompositionLocalOf { NoxDarkColors }

/** Acesso rápido aos tokens dentro de composables: `NoxTheme.colors`. */
object NoxTheme {
    val colors: NoxColors
        @Composable
        get() = LocalNoxColors.current
}

/**
 * Tema da Nox. Provê os [NoxColors] e um MaterialTheme base (para componentes
 * material3 como TextField herdarem cores coerentes).
 */
@Composable
fun NoxTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dark) NoxDarkColors else NoxLightColors
    val material =
        if (dark) {
            darkColorScheme(
                primary = colors.accent,
                background = colors.bg,
                surface = colors.surface,
                onSurface = colors.text,
                error = colors.danger,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                background = colors.bg,
                surface = colors.surface,
                onSurface = colors.text,
                error = colors.danger,
            )
        }
    CompositionLocalProvider(LocalNoxColors provides colors) {
        MaterialTheme(colorScheme = material, content = content)
    }
}
