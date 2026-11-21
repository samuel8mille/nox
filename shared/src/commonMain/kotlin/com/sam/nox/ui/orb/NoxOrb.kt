package com.sam.nox.ui.orb

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = 6.2831855f

// Olhos escuros (design v3): gradiente do topo (azulado) ao fundo (quase preto).
private val EYE_INK = Color(0xFF0B0C22)
private val EYE_TOP = Color(0xFF2A2D5E)

/** Órbita de cada partícula: raio (fração do size), período (ms), sentido, fase (0..1), tamanho (fração). */
private data class Particle(val radius: Float, val periodMs: Int, val dir: Int, val phase: Float, val size: Float)

// Raio em fração do size (derivado do inset -28% do container + inset próprio
// de cada .orb-p no CSS), período (ms), sentido, fase (0..1) e tamanho do ponto.
private val PARTICLES =
    listOf(
        Particle(0.749f, 14000, +1, 0.00f, 0.022f),
        Particle(0.640f, 19000, -1, 0.68f, 0.016f),
        Particle(0.811f, 23000, +1, 0.48f, 0.022f),
        Particle(0.562f, 12000, -1, 0.25f, 0.014f),
        Particle(0.702f, 27000, +1, 0.63f, 0.018f),
        Particle(0.874f, 17000, -1, 0.53f, 0.022f),
    )

/**
 * Rosto da Nox: orbe animado que reage ao [mood] (design v2, OrbStates.dc.html).
 *
 * @param mood estado expressivo atual.
 * @param size diâmetro do orbe.
 * @param look olhar — deslocamento do rosto seguindo o cursor, em dp (±14 x / ±10 y).
 * @param poked true por ~700ms após o clique no orbe: squash & stretch + sorrisão.
 * @param compact mini-orbe (28dp): só glow, núcleo, swirl, plasma A, rim e sheen.
 */
@Composable
fun NoxOrb(
    mood: NoxMood,
    size: Dp,
    modifier: Modifier = Modifier,
    look: Offset = Offset.Zero,
    poked: Boolean = false,
    compact: Boolean = false,
) {
    val spec = specFor(mood)

    // Transições suaves de cor (~400ms) e de dimensões dos olhos (~350ms).
    val c1 by animateColorAsState(spec.c1, tween(400), label = "c1")
    val c2 by animateColorAsState(spec.c2, tween(400), label = "c2")
    val c3 by animateColorAsState(spec.c3, tween(400), label = "c3")
    val eyeHeight by animateFloatAsState(spec.eyeHeight, tween(350), label = "eyeH")
    val eyeWidth by animateFloatAsState(spec.eyeWidth, tween(350), label = "eyeW")
    val glowAlphaBase by animateFloatAsState(spec.glowAlpha, tween(400), label = "glowA")
    val ticksAlpha by animateFloatAsState(spec.ticksAlpha, tween(500), label = "ticksA")
    val particleScale by animateFloatAsState(spec.particleScale, tween(800), label = "pScale")
    val particleAlpha by animateFloatAsState(spec.particleAlpha, tween(600), label = "pAlpha")
    val poke by animateFloatAsState(
        if (poked) 1f else 0f,
        tween(if (poked) 600 else 300, easing = FastOutSlowInEasing),
        label = "poke",
    )

    val t = rememberInfiniteTransition(label = "orb")

    val float by t.loop(6000, "float", reverse = true) // flutuação vertical
    val pool by t.loop(6000, "pool", reverse = true) // reflexo
    val glowPulse by t.loop(spec.bodyMillis.coerceAtLeast(2000), "glow", reverse = true)
    val body by t.loop(spec.bodyMillis, "body", reverse = true) // respiração/pulso
    val swirl by t.loop(spec.swirlMillis, "swirl")
    val ticks by t.loop(spec.ticksMillis, "ticks")
    val orbit by t.loop(1800, "orbit") // satélite
    val ring by t.loop(if (mood == NoxMood.Speaking) 1200 else 2200, "ring")
    val blobA by t.loop(
        if (mood == NoxMood.Thinking) {
            4000
        } else if (mood == NoxMood.Speaking) {
            3000
        } else {
            9000
        },
        "blobA",
    )
    val blobB by t.loop(if (mood == NoxMood.Speaking) 3000 else 12000, "blobB")
    val cycle by t.loop(5000, "cycle") // piscar / glance
    val particleSpin by t.loop(16000, "pSpin") // giro base do grupo de partículas
    val twinkle by t.loop(2600, "twinkle")
    val mouthT by t.loop(
        if (mood == NoxMood.Speaking) {
            1400
        } else if (mood == NoxMood.Listening) {
            2200
        } else {
            3000
        },
        "mouth",
    )
    val errorT by t.loop(2400, "errorT") // tremida + glitch + flicker
    val haloRot by t.loop(16000, "haloRot") // rotação base do halo
    val haloVoice by t.loop(if (mood == NoxMood.Speaking) 1600 else 2200, "haloVoice") // pulso tipo voz
    val haloNode by t.loop(2600, "haloNode") // nó de luz correndo (Thinking)

    Canvas(
        spec = spec,
        c1 = c1,
        c2 = c2,
        c3 = c3,
        eyeHeight = eyeHeight,
        eyeWidth = eyeWidth,
        glowAlphaBase = glowAlphaBase,
        ticksAlpha = ticksAlpha,
        particleScale = particleScale,
        particleAlpha = particleAlpha,
        poke = poke,
        mood = mood,
        look = look,
        compact = compact,
        float = float,
        pool = pool,
        glowPulse = glowPulse,
        body = body,
        swirl = swirl,
        ticks = ticks,
        orbit = orbit,
        ring = ring,
        blobA = blobA,
        blobB = blobB,
        cycle = cycle,
        particleSpin = particleSpin,
        twinkle = twinkle,
        mouthT = mouthT,
        errorT = errorT,
        haloRot = haloRot,
        haloVoice = haloVoice,
        haloNode = haloNode,
        modifier = modifier.size(size),
    )
}

/** Wrapper em torno do Canvas, só para manter a chamada acima legível. */
@Composable
private fun Canvas(
    spec: MoodSpec,
    c1: Color,
    c2: Color,
    c3: Color,
    eyeHeight: Float,
    eyeWidth: Float,
    glowAlphaBase: Float,
    ticksAlpha: Float,
    particleScale: Float,
    particleAlpha: Float,
    poke: Float,
    mood: NoxMood,
    look: Offset,
    compact: Boolean,
    float: Float,
    pool: Float,
    glowPulse: Float,
    body: Float,
    swirl: Float,
    ticks: Float,
    orbit: Float,
    ring: Float,
    blobA: Float,
    blobB: Float,
    cycle: Float,
    particleSpin: Float,
    twinkle: Float,
    mouthT: Float,
    errorT: Float,
    haloRot: Float,
    haloVoice: Float,
    haloNode: Float,
    modifier: Modifier,
) {
    androidx.compose.foundation.Canvas(modifier) {
        val s = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val lookPx = Offset(look.x * density, look.y * density)

        // Tremida horizontal no Error (nox-shake-loop): ativa só na cauda do ciclo.
        val shakeX = if (mood == NoxMood.Error) errorShake(errorT) * s else 0f
        // Flutuação do grupo (sobe até -3,5% do size). No Error é mais lenta (já no período).
        val floatY = if (compact) 0f else -(float) * s * 0.035f

        // --- Reflexo (pool), fora do grupo flutuante ---
        if (!compact) drawPool(center, s, c1, pool, dim = mood == NoxMood.Error)

        val groupOffset = Offset(shakeX, floatY)

        // Parallax de profundidade seguindo o cursor (orb-lean no design):
        // glow move pouco (fundo); partículas movem mais que o olhar (primeiro plano).
        val glowLean = Offset(lookPx.x * 0.5f, lookPx.y * 0.5f)
        val partLean = Offset(lookPx.x * 1.3f, lookPx.y * 1.3f)

        // --- Glow externo ---
        val glowAlpha =
            if (mood == NoxMood.Error) {
                glowAlphaBase
            } else {
                glowAlphaBase * (0.9f + 0.1f * glowPulse) + poke * (1f - glowAlphaBase)
            }
        drawGlow(center + groupOffset + glowLean, s, c1, glowAlpha)

        if (!compact) {
            // --- Ticks (anel de marcações) ---
            if (ticksAlpha > 0.01f) drawTicks(center + groupOffset, s, c1, ticksAlpha, ticks * 360f * spec.ticksDir)
            // --- Anéis de onda ---
            when (spec.ringMode) {
                1 -> { // ripple (Listening) 3 anéis com atrasos 0 / 0.33 / 0.66
                    drawWaveRing(center + groupOffset, s, c1, frac(ring), maxScale = 1.6f, stroke = 2f)
                    drawWaveRing(center + groupOffset, s, c1, frac(ring + 0.33f), maxScale = 1.6f, stroke = 2f)
                    drawWaveRing(center + groupOffset, s, c1, frac(ring + 0.66f), maxScale = 1.6f, stroke = 2f)
                }
                2 -> { // wave (Speaking) atrasos 0 / 0.33 / 0.66
                    drawWaveRing(center + groupOffset, s, c1, frac(ring), maxScale = 1.45f, stroke = 1.5f)
                    drawWaveRing(center + groupOffset, s, c1, frac(ring + 0.33f), maxScale = 1.45f, stroke = 1.5f)
                    drawWaveRing(center + groupOffset, s, c1, frac(ring + 0.66f), maxScale = 1.45f, stroke = 1.5f)
                }
            }
            // --- Satélite + rastro (Thinking) — mantido como fallback legado ---
            if (spec.satellite) drawSatellite(center + groupOffset, s, c1, orbit)
            // --- Halo de 3 anéis (Listening/Speaking líquido · Thinking giroscópio) ---
            if (spec.haloMode > 0) {
                drawHalo(center + groupOffset, s, c1, spec.haloMode, spec.haloIntensity, haloRot, haloVoice, haloNode)
            }
            // --- Partículas (seguem o cursor com parallax de primeiro plano) ---
            drawParticles(
                center + groupOffset + partLean,
                s,
                c1,
                particleScale * (1f + poke * 0.3f),
                particleAlpha,
                particleSpin,
                twinkle,
                mood,
            )
        }

        // --- Corpo (respiração/pulso + squash do poke) ---
        val breathe =
            when (mood) {
                NoxMood.Speaking -> 1f + body * 0.05f
                else -> 1f + body * 0.035f
            }
        val sx = breathe * (1f + poke * squashX(poke))
        val sy = breathe * (1f + poke * squashY(poke))
        drawBody(
            center = center + groupOffset,
            s = s,
            bodyScaleX = sx,
            bodyScaleY = sy,
            c1 = c1,
            c2 = c2,
            c3 = c3,
            swirlAngle = swirl * 360f,
            swirlAlpha = spec.swirlAlpha,
            blobA = blobA,
            blobB = blobB,
            look = lookPx,
            mood = mood,
            errorT = errorT,
            compact = compact,
        )

        if (compact) return@Canvas

        // --- Rosto ---
        drawFace(
            center = center + groupOffset + lookPx,
            s = s,
            eyeHeight = eyeHeight,
            eyeWidth = eyeWidth,
            mood = mood,
            spec = spec,
            cycle = cycle,
            mouthT = mouthT,
            errorT = errorT,
            poke = poke,
            c1 = c1,
            c3 = c3,
        )

        // --- Faíscas do poke (por cima de tudo): onda de choque + estrelinhas ---
        if (poke > 0.01f) drawPokeSparks(center + groupOffset, s, c1, poke)
    }
}

// ----------------------------------------------------------------------------
// Camadas de desenho
// ----------------------------------------------------------------------------

private fun DrawScope.drawPool(center: Offset, s: Float, c1: Color, pool: Float, dim: Boolean) {
    val alpha = if (dim) 0.2f else (0.5f - pool * 0.18f)
    val scaleX = 1f - pool * 0.16f
    val w = s * 0.72f * scaleX
    val h = s * 0.13f
    val cx = center.x
    val cy = center.y + s * 0.57f
    drawOval(
        brush =
        Brush.radialGradient(
            listOf(c1.copy(alpha = alpha), Color.Transparent),
            center = Offset(cx, cy),
            radius = w / 2f,
        ),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h),
    )
}

private fun DrawScope.drawGlow(center: Offset, s: Float, c1: Color, alpha: Float) {
    val r = s * 0.85f
    drawCircle(
        brush = Brush.radialGradient(listOf(c1.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent), center, r),
        radius = r,
        center = center,
    )
}

private fun DrawScope.drawTicks(center: Offset, s: Float, c1: Color, alpha: Float, angleDeg: Float) {
    val r = s * 0.67f
    val stroke = Stroke(width = s * 0.03f)
    rotate(angleDeg, pivot = center) {
        var a = 0f
        while (a < 360f) {
            val rad = a / 360f * TWO_PI
            val p = Offset(center.x + cos(rad) * r, center.y + sin(rad) * r)
            drawCircle(c1.copy(alpha = alpha), radius = s * 0.008f, center = p)
            a += 7.5f
        }
    }
    // mantém o stroke importado "em uso" sem afetar o traço de pontos
    if (stroke.width < 0f) drawCircle(Color.Transparent, 0f, center)
}

private fun DrawScope.drawWaveRing(center: Offset, s: Float, c1: Color, prog: Float, maxScale: Float, stroke: Float) {
    val scale = 1f + prog * (maxScale - 1f)
    val alpha = ((if (maxScale > 1.5f) 0.55f else 0.6f) * (1f - prog)).coerceIn(0f, 1f)
    if (alpha <= 0f) return
    drawCircle(
        color = c1.copy(alpha = alpha),
        radius = s / 2f * scale,
        center = center,
        style = Stroke(width = stroke.dp.toPx()),
    )
}

private fun DrawScope.drawSatellite(center: Offset, s: Float, c1: Color, orbit: Float) {
    val r = s * 0.59f
    val baseAngle = orbit * 360f
    // rastro: arco de 120° atrás do satélite
    val sweep = 120f
    drawArc(
        brush =
        Brush.sweepGradient(
            0f to Color.Transparent,
            0.9f to Color.Transparent,
            1f to c1.copy(alpha = 0.8f),
            center = center,
        ),
        startAngle = baseAngle - 90f - sweep,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = s * 0.02f),
    )
    val rad = (baseAngle - 90f) / 360f * TWO_PI
    val p = Offset(center.x + cos(rad) * r, center.y + sin(rad) * r)
    drawCircle(c1.copy(alpha = 0.6f), radius = s * 0.05f, center = p)
    drawCircle(Color.White, radius = s * 0.0225f, center = p)
}

/**
 * Halo de 3 anéis em volta do orbe.
 * - [mode] 1: anéis "líquidos" (elipses que rodam e pulsam no ritmo da fala).
 * - [mode] 2: giroscópio 3D (anéis inclinados em perspetiva) com um nó de luz correndo.
 */
private fun DrawScope.drawHalo(
    center: Offset,
    s: Float,
    c1: Color,
    mode: Int,
    intensity: Float,
    rot: Float,
    voice: Float,
    node: Float,
) {
    // 3 anéis com raios/opacidades/velocidades diferentes.
    val rings =
        listOf(
            Triple(0.59f, 0.55f, 1f), // inset -9%  → r≈0.59·s
            Triple(0.64f, 0.38f, 1.6f), // inset -14%
            Triple(0.69f, 0.22f, 2.2f), // inset -19%
        )
    rings.forEachIndexed { i, (baseR, baseOp, k) ->
        val op = (baseOp * intensity).coerceIn(0f, 1f)
        val stroke = s * (0.008f - i * 0.0015f)
        if (mode == 2) {
            // Giroscópio: cada anel é uma elipse achatada num eixo, inclinação distinta.
            val rr = (baseR + 0.03f) * s
            val tilt =
                when (i) {
                    0 -> 0.52f
                    1 -> 0.52f
                    else -> 0.6f
                } // achatamento vertical
            val angle = (rot * 360f * (if (i == 1) -1f else 1f) * (1f + i * 0.2f))
            rotate(angle + i * 40f, pivot = center) {
                drawOval(
                    color = c1.copy(alpha = op),
                    topLeft = Offset(center.x - rr, center.y - rr * tilt),
                    size = Size(rr * 2f, rr * 2f * tilt),
                    style = Stroke(width = stroke),
                )
            }
            // Nó de luz correndo pelo anel.
            val na = frac(node + i * 0.33f) * TWO_PI
            val np =
                Offset(
                    center.x + cos(na) * rr,
                    center.y + sin(na) * rr * tilt,
                )
            rotate(angle + i * 40f, pivot = center) {
                drawCircle(Color.White, radius = s * 0.02f, center = np)
                drawCircle(c1.copy(alpha = 0.7f), radius = s * 0.035f, center = np)
            }
        } else {
            // Líquido: anel pulsando no ritmo (voice) com leve excentricidade giratória.
            val amp = 0.35f
            val pulse = 1f + haloVoiceScale(frac(voice + i * 0.3f)) * amp * k * 0.08f
            val rr = baseR * s * pulse
            // elipse levemente excêntrica que roda para dar o aspeto "líquido".
            val ecc = 0.04f * sin((rot + i * 0.2f) * TWO_PI)
            rotate(rot * 360f * (if (i == 1) -1f else 1f) / (1f + i), pivot = center) {
                drawOval(
                    color = c1.copy(alpha = op),
                    topLeft = Offset(center.x - rr * (1f + ecc), center.y - rr * (1f - ecc)),
                    size = Size(rr * 2f * (1f + ecc), rr * 2f * (1f - ecc)),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

/** Curva de "voz" do halo (picos irregulares, como amplitude de fala). */
private fun haloVoiceScale(t: Float): Float = when {
    t < 0.14f -> t / 0.14f
    t < 0.28f -> 1f - (t - 0.14f) / 0.14f * 0.8f
    t < 0.42f -> 0.2f + (t - 0.28f) / 0.14f * 0.8f
    t < 0.58f -> 1f - (t - 0.42f) / 0.16f * 0.73f
    t < 0.72f -> 0.27f + (t - 0.58f) / 0.14f * 0.53f
    t < 0.86f -> 0.8f - (t - 0.72f) / 0.14f * 0.65f
    else -> 0.15f + (t - 0.86f) / 0.14f * 0.85f
}

/** Faíscas da cutucada: onda de choque + 10 estrelinhas radiais. */
private fun DrawScope.drawPokeSparks(center: Offset, s: Float, c1: Color, poke: Float) {
    // Onda de choque: anel que expande 1→2.2 e some (vida ~0.75 do poke).
    val shockT = (poke / 0.75f).coerceIn(0f, 1f)
    if (shockT < 1f) {
        val scale = 1f + shockT * 1.2f
        val alpha = (1f - shockT) * 0.8f
        val color = lerpColor(Color.White, c1, shockT)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = s * 0.5f * scale,
            center = center,
            style = Stroke(width = s * 0.012f),
        )
    }
    // Burst: 10 pontos radiais, cada um com comprimento próprio.
    val lens = floatArrayOf(0.3f, 0.42f, 0.26f, 0.38f, 0.33f, 0.44f, 0.28f, 0.4f, 0.31f, 0.36f)
    val burstT = (poke / 0.85f).coerceIn(0f, 1f)
    val easeOut = 1f - (1f - burstT) * (1f - burstT)
    for (i in 0 until 10) {
        val ang = (i * 36f - 90f) / 360f * TWO_PI
        val dist = s * (0.52f + lens[i] * easeOut)
        val p = Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist)
        val alpha = (1f - burstT)
        val sz = s * 0.016f * (1f - burstT * 0.8f)
        drawCircle(c1.copy(alpha = alpha * 0.6f), radius = sz * 2f, center = p)
        drawCircle(Color.White.copy(alpha = alpha), radius = sz, center = p)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

private fun DrawScope.drawParticles(
    center: Offset,
    s: Float,
    c1: Color,
    groupScale: Float,
    groupAlpha: Float,
    spin: Float,
    twinkle: Float,
    mood: NoxMood,
) {
    val fall = if (mood == NoxMood.Error) s * 0.1f else 0f
    val groupSpin = if (mood == NoxMood.Thinking) spin * TWO_PI * 4f else 0f // grupo gira em 4s
    PARTICLES.forEachIndexed { i, p ->
        val own = (spin * 16000f / p.periodMs) * p.dir // volta própria
        val angle = own * TWO_PI + p.phase * TWO_PI + groupSpin
        val r = p.radius * s * groupScale
        val px = center.x + cos(angle) * r
        val py = center.y + sin(angle) * r + fall
        // twinkle: alpha 0.25↔1, escala 0.6↔1 (fase por índice)
        val tw = frac(twinkle + i * 0.17f)
        val ta = 0.25f + 0.75f * tri(tw)
        val tscale = 0.6f + 0.4f * tri(tw)
        val alpha = if (mood == NoxMood.Error) groupAlpha else groupAlpha * ta
        val color = if (mood == NoxMood.Error) c1 else Color.White
        drawCircle(c1.copy(alpha = alpha * 0.5f), radius = p.size * s * 1.6f * tscale, center = Offset(px, py))
        drawCircle(color.copy(alpha = alpha), radius = p.size * s * tscale, center = Offset(px, py))
    }
}

private fun DrawScope.drawBody(
    center: Offset,
    s: Float,
    bodyScaleX: Float,
    bodyScaleY: Float,
    c1: Color,
    c2: Color,
    c3: Color,
    swirlAngle: Float,
    swirlAlpha: Float,
    blobA: Float,
    blobB: Float,
    look: Offset,
    mood: NoxMood,
    errorT: Float,
    compact: Boolean,
) {
    val rx = s / 2f * bodyScaleX
    val ry = s / 2f * bodyScaleY
    val r = (rx + ry) / 2f

    // flicker do Error: clareia/escurece o núcleo em picos no fim do ciclo.
    val flicker = if (mood == NoxMood.Error) errorFlicker(errorT) else 1f

    // Recorta tudo ao círculo do corpo.
    val clip =
        Path().apply {
            addOval(Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry))
        }
    clipPath(clip) {
        // Núcleo
        val coreCenter = center + Offset(-r * 0.16f, -r * 0.22f)
        drawCircle(
            brush =
            Brush.radialGradient(
                0f to lerpWhite(c1, flicker, 0.9f),
                0.20f to scaleBright(c1, flicker),
                0.58f to scaleBright(c2, flicker),
                1f to c3,
                center = coreCenter,
                radius = r * 1.25f,
            ),
            radius = r,
            center = center,
        )
        // Redemoinho (swirl)
        rotate(swirlAngle, pivot = center) {
            drawCircle(
                brush =
                Brush.sweepGradient(
                    0f to Color.Transparent,
                    70f / 360f to c1.copy(alpha = swirlAlpha),
                    150f / 360f to Color.Transparent,
                    230f / 360f to c2.copy(alpha = swirlAlpha),
                    320f / 360f to Color.Transparent,
                    1f to Color.Transparent,
                    center = center,
                ),
                radius = r,
                center = center,
                blendMode = BlendMode.Screen,
            )
        }
        // Plasma A
        drawBlob(
            center,
            r,
            blobA,
            radial = listOf(c1.copy(alpha = 0.75f), Color.Transparent),
            path = listOf(Offset(-0.18f, -0.10f) to 1f, Offset(0.20f, 0.08f) to 1.15f, Offset(-0.04f, 0.22f) to 0.9f),
        )
        if (!compact) {
            // Plasma B
            drawBlob(
                center,
                r,
                blobB,
                radial = listOf(Color.White.copy(alpha = 0.45f), c2.copy(alpha = 0.45f), Color.Transparent),
                path = listOf(
                    Offset(0.16f, 0.14f) to 1f,
                    Offset(-0.20f, 0.04f) to 0.85f,
                    Offset(0.08f, -0.22f) to 1.2f,
                ),
            )
        }
        // Glitch (Error)
        if (mood == NoxMood.Error) {
            val g = errorGlitch(errorT)
            if (g > 0f) {
                val gc = Offset(center.x + r * 0.24f - r * 0.06f, center.y - r * 0.16f)
                drawCircle(
                    brush =
                    Brush.radialGradient(
                        listOf(Color(0xFF5CE1E6).copy(alpha = 0.6f * g), Color.Transparent),
                        center = gc,
                        radius = r * 0.55f,
                    ),
                    radius = r,
                    center = center,
                    blendMode = BlendMode.Screen,
                )
            }
        }
        // Sombra interna embaixo
        drawCircle(
            brush =
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                startY = center.y,
                endY = center.y + ry,
            ),
            radius = r,
            center = center,
        )
    }

    // Rim light (borda interna): anel em c1 + linha branca.
    drawCircle(c1.copy(alpha = 0.5f), radius = r, center = center, style = Stroke(width = s * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.18f), radius = r - 1f, center = center, style = Stroke(width = 1.dp.toPx()))

    // Sheen (paralaxe inversa ao olhar)
    val sheenCenter =
        Offset(
            center.x - r * 0.44f - look.x * 0.6f,
            center.y - r * 0.62f - look.y * 0.6f,
        )
    val sheenRx = s * 0.18f
    val sheenRy = s * 0.10f
    drawOval(
        brush =
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
            center = sheenCenter,
            radius = maxOf(sheenRx, sheenRy),
        ),
        topLeft = Offset(sheenCenter.x - sheenRx, sheenCenter.y - sheenRy),
        size = Size(sheenRx * 2f, sheenRy * 2f),
    )
}

private fun DrawScope.drawBlob(
    center: Offset,
    r: Float,
    prog: Float,
    radial: List<Color>,
    path: List<Pair<Offset, Float>>,
) {
    // Interpola a posição/escala entre 3 keyframes (0→33→66→volta).
    val (off, scale) = sample3(path, prog)
    val bc = Offset(center.x + off.x * r, center.y + off.y * r)
    val br = r * 0.7f * scale
    drawCircle(
        brush = Brush.radialGradient(radial, center = bc, radius = br),
        radius = br,
        center = bc,
        blendMode = BlendMode.Screen,
    )
}

// ----------------------------------------------------------------------------
// Rosto
// ----------------------------------------------------------------------------

private fun DrawScope.drawFace(
    center: Offset,
    s: Float,
    eyeHeight: Float,
    eyeWidth: Float,
    mood: NoxMood,
    spec: MoodSpec,
    cycle: Float,
    mouthT: Float,
    errorT: Float,
    poke: Float,
    c1: Color,
    c3: Color,
) {
    // Deslocamento do olhar interno (glance/ponder) por estado.
    val eyeOffset =
        when {
            poke > 0.01f -> Offset.Zero
            mood == NoxMood.Idle -> glance(cycle) * s
            mood == NoxMood.Thinking -> ponder(frac(mouthT)) * s // mesmo ritmo (3s) da boca "hmm"
            else -> Offset.Zero
        }
    // Piscar duplo (Idle/Thinking seguem o ciclo; outros não piscam).
    val blink = if ((mood == NoxMood.Idle) && poke < 0.01f) doubleBlink(cycle) else 1f
    val eyeH = if (poke > 0.01f) s * 0.035f else s * eyeHeight * blink
    val eyeW = s * eyeWidth
    val gap = s * 0.13f
    val cy = center.y + eyeOffset.y - s * 0.09f // olhos acima do centro (coluna com gap)
    val lx = center.x - gap / 2f - eyeW / 2f + eyeOffset.x
    val rx = center.x + gap / 2f + eyeW / 2f + eyeOffset.x
    val showGlints = mood != NoxMood.Error
    eye(Offset(lx, cy), eyeW, eyeH, showGlints)
    eye(Offset(rx, cy), eyeW, eyeH, showGlints)

    // Boca logo abaixo dos olhos (coluna com gap de ~4,5% do size, como no protótipo).
    // Acompanha o deslocamento vertical do olhar, mas não o lateral.
    val mouthY = cy + eyeH / 2f + s * 0.045f + s * 0.03f
    val mouthCenter = Offset(center.x, mouthY)
    val kind = if (poke > 0.01f) MouthKind.Poke else spec.mouth
    drawMouth(kind, mouthCenter, s, mouthT, errorT, c1, c3)
}

/** Olho escuro: pílula com gradiente vertical e dois brilhos brancos. */
private fun DrawScope.eye(center: Offset, w: Float, h: Float, glints: Boolean) {
    val radius = minOf(w, h) / 2f
    val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
    drawRoundRect(
        brush =
        Brush.verticalGradient(
            0f to EYE_TOP,
            0.6f to EYE_INK,
            1f to EYE_INK,
            startY = topLeft.y,
            endY = topLeft.y + h,
        ),
        topLeft = topLeft,
        size = Size(w, h),
        cornerRadius = CornerRadius(radius, radius),
    )
    if (!glints || h < w) return // em linha fina (Error/semicerrado) não desenha brilho
    // Brilho principal (canto sup-esq) e secundário (inf-dir).
    val g1 = Offset(center.x - w * 0.18f, center.y - h * 0.28f)
    drawOval(
        Color.White.copy(alpha = 0.9f),
        topLeft = Offset(g1.x - w * 0.23f, g1.y - h * 0.12f),
        size = Size(w * 0.46f, h * 0.24f),
    )
    val g2 = Offset(center.x + w * 0.16f, center.y + h * 0.26f)
    drawOval(
        Color.White.copy(alpha = 0.5f),
        topLeft = Offset(g2.x - w * 0.13f, g2.y - h * 0.07f),
        size = Size(w * 0.26f, h * 0.14f),
    )
}

private fun DrawScope.drawMouth(
    kind: MouthKind,
    center: Offset,
    s: Float,
    mouthT: Float,
    errorT: Float,
    c1: Color,
    c3: Color,
) {
    when (kind) {
        MouthKind.Smile -> {
            // arco inferior (sorriso leve): borda de baixo de um retângulo arredondado.
            val w = s * 0.13f
            val h = s * 0.05f
            arcBottom(center, w, h, s * 0.016f, EYE_INK)
        }
        MouthKind.O -> {
            val pulse = 1f + tri(frac(mouthT)) * 0.15f
            val d = s * 0.07f * pulse
            drawCircle(c3, radius = d / 2f, center = center)
            drawCircle(EYE_INK, radius = d / 2f, center = center, style = Stroke(width = s * 0.014f))
        }
        MouthKind.Hmm -> {
            // "hmm" de lado: traço fino que desliza e inclina (nox-hmm: 3 keyframes).
            val (shiftFrac, tilt) = hmmPose(frac(mouthT))
            val w = s * 0.07f
            val h = s * 0.022f
            val mc = Offset(center.x + shiftFrac * s, center.y)
            rotate(tilt, pivot = mc) {
                pill(mc, w, h, EYE_INK)
            }
        }
        MouthKind.Speak -> {
            // boca aberta que muda de forma (fonemas) + língua.
            val (mw, mh) = speakShape(mouthT, s)
            drawRoundRect(
                color = c3,
                topLeft = Offset(center.x - mw / 2f, center.y - mh / 2f),
                size = Size(mw, mh),
                cornerRadius = CornerRadius(mw * 0.45f, mh * 0.45f),
            )
            drawRoundRect(
                color = EYE_INK,
                topLeft = Offset(center.x - mw / 2f, center.y - mh / 2f),
                size = Size(mw, mh),
                cornerRadius = CornerRadius(mw * 0.45f, mh * 0.45f),
                style = Stroke(width = s * 0.013f),
            )
            // língua (elipse c1 na parte de baixo).
            val tw = mw * 0.56f
            val th = mh * 0.42f
            val tc = Offset(center.x, center.y + mh * 0.5f - th * 0.7f)
            drawOval(
                c1.copy(alpha = 0.85f),
                topLeft = Offset(tc.x - tw / 2f, tc.y - th / 2f),
                size = Size(tw, th),
            )
        }
        MouthKind.Sad -> {
            // arco superior (triste) balançando.
            val w = s * 0.12f
            val h = s * 0.045f
            val wob = (tri(frac(errorT)) - 0.5f) * 2f // leve
            arcTop(Offset(center.x, center.y + s * 0.015f), w, h, s * 0.016f, EYE_INK, wob * 0.1f)
        }
        MouthKind.Poke -> {
            // sorrisão aberto "D".
            val w = s * 0.18f
            val h = s * 0.085f
            drawRoundRect(
                color = c3,
                topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.12f, h * 0.5f),
            )
            drawRoundRect(
                color = EYE_INK,
                topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.12f, h * 0.5f),
                style = Stroke(width = s * 0.013f),
            )
            val tw = w * 0.56f
            val th = h * 0.42f
            val tc = Offset(center.x, center.y + h * 0.5f - th * 0.7f)
            drawOval(
                c1.copy(alpha = 0.85f),
                topLeft = Offset(tc.x - tw / 2f, tc.y - th / 2f),
                size = Size(tw, th),
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Helpers geométricos e de animação
// ----------------------------------------------------------------------------

private fun DrawScope.pill(center: Offset, w: Float, h: Float, color: Color) {
    val radius = minOf(w, h) / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(radius, radius),
    )
}

/** Borda inferior de um retângulo arredondado (sorriso). */
private fun DrawScope.arcBottom(center: Offset, w: Float, h: Float, stroke: Float, color: Color) {
    val path =
        Path().apply {
            moveTo(center.x - w / 2f, center.y - h * 0.1f)
            quadraticBezierTo(center.x, center.y + h, center.x + w / 2f, center.y - h * 0.1f)
        }
    drawPath(path, color, style = Stroke(width = stroke))
}

/** Arco superior (boca triste), com leve rotação. */
private fun DrawScope.arcTop(center: Offset, w: Float, h: Float, stroke: Float, color: Color, tilt: Float) {
    rotate(tilt * 7f, pivot = center) {
        val path =
            Path().apply {
                moveTo(center.x - w / 2f, center.y + h * 0.1f)
                quadraticBezierTo(center.x, center.y - h, center.x + w / 2f, center.y + h * 0.1f)
            }
        drawPath(path, color, style = Stroke(width = stroke))
    }
}

private fun frac(v: Float): Float = v - kotlin.math.floor(v)

/** Onda triangular 0→1→0 para t em [0,1). */
private fun tri(t: Float): Float = 1f - abs(t - 0.5f) * 2f

/** Posição/escala interpolada entre 3 keyframes (vai e volta ao primeiro). */
private fun sample3(path: List<Pair<Offset, Float>>, prog: Float): Pair<Offset, Float> {
    // 0→0.33 (a→b), 0.33→0.66 (b→c), 0.66→1 (c→a)
    val (a, sa) = path[0]
    val (b, sb) = path[1]
    val (c, sc) = path[2]
    return when {
        prog < 0.33f -> lerpOS(a, sa, b, sb, prog / 0.33f)
        prog < 0.66f -> lerpOS(b, sb, c, sc, (prog - 0.33f) / 0.33f)
        else -> lerpOS(c, sc, a, sa, (prog - 0.66f) / 0.34f)
    }
}

private fun lerpOS(a: Offset, sa: Float, b: Offset, sb: Float, t: Float): Pair<Offset, Float> =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t) to (sa + (sb - sa) * t)

/** Glance dos olhos (Idle): centro → esquerda → direita/cima → centro, em 9s. */
private fun glance(cycle: Float): Offset {
    val t = cycle // 0..1 ~ 5s; aproximamos o padrão de 9s em cima do ciclo de 5s
    return when {
        t < 0.38f -> Offset.Zero
        t < 0.52f -> Offset(-0.05f, 0f)
        t < 0.66f -> Offset(0.05f, -0.02f)
        else -> Offset.Zero
    }
}

/** Ponder (Thinking): olhar vaga para cima, interpolado (nox-ponder, 3s). */
private fun ponder(cycle: Float): Offset {
    // keyframes: 0% (.05,-.07) → 33% (-.05,-.07) → 66% (.01,-.03) → volta
    val a = Offset(0.05f, -0.07f)
    val b = Offset(-0.05f, -0.07f)
    val c = Offset(0.01f, -0.03f)
    return when {
        cycle < 0.33f -> lerpOffset(a, b, smooth(cycle / 0.33f))
        cycle < 0.66f -> lerpOffset(b, c, smooth((cycle - 0.33f) / 0.33f))
        else -> lerpOffset(c, a, smooth((cycle - 0.66f) / 0.34f))
    }
}

/** Pose da boca "hmm": desliza e inclina, interpolado (nox-hmm, 3s). */
private fun hmmPose(t: Float): Pair<Float, Float> {
    // keyframes: 0% (.04,-8°) → 33% (-.03,6°) → 66% (.01,-3°) → volta
    val sa = 0.04f
    val ra = -8f
    val sb = -0.03f
    val rb = 6f
    val sc = 0.01f
    val rc = -3f
    return when {
        t < 0.33f -> lerp2(sa, ra, sb, rb, smooth(t / 0.33f))
        t < 0.66f -> lerp2(sb, rb, sc, rc, smooth((t - 0.33f) / 0.33f))
        else -> lerp2(sc, rc, sa, ra, smooth((t - 0.66f) / 0.34f))
    }
}

private fun lerp2(a1: Float, a2: Float, b1: Float, b2: Float, k: Float): Pair<Float, Float> =
    (a1 + (b1 - a1) * k) to (a2 + (b2 - a2) * k)

private fun lerpOffset(a: Offset, b: Offset, k: Float): Offset = Offset(a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k)

/** Ease in-out suave (como ease-in-out do CSS). */
private fun smooth(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Piscar duplo (dois fechamentos rápidos perto do fim do ciclo). */
private fun doubleBlink(cycle: Float): Float {
    fun closeAt(c: Float, at: Float): Float {
        val d = abs(c - at)
        return if (d < 0.015f) 0.1f + (d / 0.015f) * 0.9f else 1f
    }
    return minOf(closeAt(cycle, 0.86f), closeAt(cycle, 0.90f))
}

/** Tremida horizontal do Error (ativa na cauda do ciclo). */
private fun errorShake(t: Float): Float = when {
    t < 0.70f -> 0f
    t < 0.74f -> -0.028f
    t < 0.78f -> 0.028f
    t < 0.82f -> -0.016f
    t < 0.86f -> 0.012f
    else -> 0f
}

private fun errorFlicker(t: Float): Float = when {
    t < 0.88f -> 1f
    t < 0.90f -> 1.7f
    t < 0.92f -> 0.6f
    t < 0.94f -> 1.4f
    else -> 1f
}

private fun errorGlitch(t: Float): Float = when {
    t < 0.88f -> 0f
    t < 0.93f -> 0.6f
    t < 0.96f -> 0.45f
    else -> 0f
}

/** Forma da boca falando (fonemas), interpolando a tabela de keyframes. */
private fun speakShape(t: Float, s: Float): Pair<Float, Float> {
    val kf =
        listOf(
            0.10f to 0.05f,
            0.14f to 0.09f,
            0.08f to 0.075f,
            0.165f to 0.045f,
            0.11f to 0.105f,
            0.15f to 0.06f,
            0.07f to 0.055f,
            0.13f to 0.085f,
        )
    val n = kf.size
    val p = frac(t) * n
    val i = p.toInt() % n
    val j = (i + 1) % n
    val f = p - p.toInt()
    val w = (kf[i].first + (kf[j].first - kf[i].first) * f) * s
    val h = (kf[i].second + (kf[j].second - kf[i].second) * f) * s
    return w to h
}

// squash & stretch do poke: curva simples que vai (1.14,0.86)→(0.92,1.1)→volta.
private fun squashX(poke: Float): Float = when {
    poke < 0.25f -> 0.14f * (poke / 0.25f)
    poke < 0.5f -> 0.14f - 0.22f * ((poke - 0.25f) / 0.25f)
    poke < 0.75f -> -0.08f + 0.12f * ((poke - 0.5f) / 0.25f)
    else -> 0.04f - 0.04f * ((poke - 0.75f) / 0.25f)
}

private fun squashY(poke: Float): Float = -squashX(poke)

// Ajustes de brilho para o flicker do Error.
private fun scaleBright(c: Color, k: Float): Color {
    if (k == 1f) return c
    return Color(
        red = (c.red * k).coerceIn(0f, 1f),
        green = (c.green * k).coerceIn(0f, 1f),
        blue = (c.blue * k).coerceIn(0f, 1f),
        alpha = c.alpha,
    )
}

private fun lerpWhite(c: Color, k: Float, baseAlpha: Float): Color =
    Color.White.copy(alpha = (baseAlpha * (if (k > 1f) 1f else k)).coerceIn(0f, 1f))

/** Animação em loop 0→1 (reverse = vai-e-volta com easing suave; senão restart linear). */
@Composable
private fun InfiniteTransition.loop(periodMs: Int, label: String, reverse: Boolean = false): State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
        infiniteRepeatable(
            animation = tween(periodMs, easing = if (reverse) FastOutSlowInEasing else LinearEasing),
            repeatMode = if (reverse) RepeatMode.Reverse else RepeatMode.Restart,
        ),
        label = label,
    )
