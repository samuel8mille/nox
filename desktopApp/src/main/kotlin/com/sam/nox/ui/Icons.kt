package com.sam.nox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícones desenhados em Canvas (sem depender de material-icons-extended),
 * fiéis aos SVGs do design: microfone e seta para cima (enviar).
 */
@Composable
fun MicIcon(tint: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Corpo do microfone (retângulo arredondado), ~rect(9,3,6,11,rx3) em 24.
        val bodyLeft = w * (9f / 24f)
        val bodyTop = h * (3f / 24f)
        val bodyW = w * (6f / 24f)
        val bodyH = h * (11f / 24f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW / 2f, bodyW / 2f),
            style = stroke,
        )
        // Arco inferior: M5 11 a7 7 0 0 0 14 0
        val arc =
            Path().apply {
                moveTo(w * (5f / 24f), h * (11f / 24f))
                cubicTo(
                    w * (5f / 24f),
                    h * (16f / 24f),
                    w * (19f / 24f),
                    h * (16f / 24f),
                    w * (19f / 24f),
                    h * (11f / 24f),
                )
            }
        drawPath(arc, tint, style = stroke)
        // Haste: M12 18 v3
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * (18f / 24f)),
            end = Offset(w * 0.5f, h * (21f / 24f)),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun SendArrowIcon(tint: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Haste: M12 19 V5
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * (19f / 24f)),
            end = Offset(w * 0.5f, h * (5f / 24f)),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        // Ponta: M6 11 l6-6 l6 6
        val head =
            Path().apply {
                moveTo(w * (6f / 24f), h * (11f / 24f))
                lineTo(w * (12f / 24f), h * (5f / 24f))
                lineTo(w * (18f / 24f), h * (11f / 24f))
            }
        drawPath(head, tint, style = stroke)
    }
}
