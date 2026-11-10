package com.chungaro.combustible.nearby

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Ilustración de fondo de la cabecera: sol, colinas, una carretera hacia el
 * horizonte y la silueta de un surtidor. Dibujada en código (nítida en
 * cualquier pantalla y sin imágenes externas).
 */
fun DrawScope.drawHeaderScene() {
    val w = size.width
    val h = size.height

    // Destellos suaves
    listOf(Triple(0.12f, 0.18f, 0.10f), Triple(0.45f, 0.10f, 0.06f), Triple(0.30f, 0.55f, 0.05f)).forEach { (x, y, r) ->
        drawCircle(Color.White.copy(alpha = 0.06f), radius = h * r * 2, center = Offset(w * x, h * y))
    }

    // Sol
    val sun = Offset(w * 0.70f, h * 0.60f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x66FBBF24), Color(0x00FBBF24)), center = sun, radius = h * 0.55f),
        radius = h * 0.55f,
        center = sun,
    )
    drawCircle(Color(0xCCFCD34D), radius = h * 0.16f, center = sun)

    // Colinas lejanas
    val far = Path().apply {
        moveTo(0f, h * 0.72f)
        quadraticTo(w * 0.18f, h * 0.58f, w * 0.38f, h * 0.68f)
        quadraticTo(w * 0.60f, h * 0.78f, w * 0.80f, h * 0.62f)
        quadraticTo(w * 0.92f, h * 0.55f, w, h * 0.64f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(far, Color.White.copy(alpha = 0.10f))

    // Colinas cercanas
    val near = Path().apply {
        moveTo(0f, h * 0.86f)
        quadraticTo(w * 0.25f, h * 0.74f, w * 0.52f, h * 0.84f)
        quadraticTo(w * 0.78f, h * 0.94f, w, h * 0.80f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(near, Color(0x55042F2E))

    // Carretera hacia el horizonte
    val vanish = Offset(w * 0.70f, h * 0.66f)
    val road = Path().apply {
        moveTo(vanish.x - w * 0.006f, vanish.y)
        lineTo(vanish.x + w * 0.006f, vanish.y)
        lineTo(w * 0.86f, h)
        lineTo(w * 0.50f, h)
        close()
    }
    drawPath(road, Color(0x88083344))
    // Línea discontinua central
    val steps = 6
    for (i in 0 until steps) {
        val t0 = (i + 0.15f) / steps
        val t1 = (i + 0.55f) / steps
        fun at(t: Float) = Offset(vanish.x + (w * 0.68f - vanish.x) * t * t, vanish.y + (h - vanish.y) * t * t)
        drawLine(Color(0xCCFDE68A), at(t0), at(t1), strokeWidth = 1.5f + 5f * t1, cap = StrokeCap.Round)
    }

    // Silueta del surtidor
    val ph = h * 0.50f
    val pw = ph * 0.55f
    val left = w * 0.88f - pw / 2
    val top = h - ph - h * 0.06f
    val ink = Color.White.copy(alpha = 0.22f)
    drawRoundRect(ink, Offset(left, top), Size(pw, ph), CornerRadius(pw * 0.15f))
    drawRoundRect(Color(0x33042F2E), Offset(left + pw * 0.15f, top + ph * 0.12f), Size(pw * 0.7f, ph * 0.28f), CornerRadius(pw * 0.08f))
    drawRoundRect(ink, Offset(left - pw * 0.18f, top + ph), Size(pw * 1.36f, ph * 0.08f), CornerRadius(4f))
    val hose = Path().apply {
        moveTo(left + pw, top + ph * 0.32f)
        lineTo(left + pw * 1.18f, top + ph * 0.32f)
        lineTo(left + pw * 1.18f, top + ph * 0.80f)
        quadraticTo(left + pw * 1.35f, top + ph * 0.92f, left + pw * 1.45f, top + ph * 0.78f)
        lineTo(left + pw * 1.45f, top + ph * 0.18f)
        lineTo(left + pw * 1.28f, top + ph * 0.05f)
    }
    drawPath(hose, ink, style = Stroke(width = pw * 0.09f, cap = StrokeCap.Round))
}
