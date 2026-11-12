package com.chungaro.combustible.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/*
 * Cifras de 7 segmentos, como las de los totens de precios de los postos.
 *
 * Cada dígito ocupa 10 × 18 unidades; los segmentos son hexágonos de 2,2 de grueso y
 * todo se inclina un poco a la derecha. La coma es un cuadrado abajo. Los segmentos
 * apagados se dibujan también, muy tenues. La web usa la misma geometría.
 */
private object Seg {
    const val W = 10f
    const val H = 18f
    const val T = 2.2f
    const val G = 0.45f
    const val GAP = 3f
    const val SKEW = 0.1f

    /** Segmentos encendidos de cada cifra (bits a, b, c, d, e, f, g). */
    val DIGITS = intArrayOf(63, 6, 91, 79, 102, 109, 125, 7, 127, 111)

    private const val h = T / 2

    private fun hSeg(x0: Float, x1: Float, y: Float) =
        floatArrayOf(x0, y, x0 + h, y - h, x1 - h, y - h, x1, y, x1 - h, y + h, x0 + h, y + h)

    private fun vSeg(x: Float, y0: Float, y1: Float) =
        floatArrayOf(x, y0, x + h, y0 + h, x + h, y1 - h, x, y1, x - h, y1 - h, x - h, y0 + h)

    /** Los 7 segmentos de un dígito en el origen, en orden a…g. */
    val SHAPES = listOf(
        hSeg(h + G, W - h - G, h),
        vSeg(W - h, h + G, H / 2 - G),
        vSeg(W - h, H / 2 + G, H - h - G),
        hSeg(h + G, W - h - G, H - h),
        vSeg(h, H / 2 + G, H - h - G),
        vSeg(h, h + G, H / 2 - G),
        hSeg(h + G, W - h - G, H / 2),
    )
}

/** Un polígono en unidades del dígito, ya inclinado, y si está encendido. */
private class SegPoly(val points: FloatArray, val on: Boolean)

private class SegLayout(val width: Float, val polys: List<SegPoly>)

/** Coloca las cifras y la coma de [text] (por ejemplo «1,963»). Ignora el resto. */
private fun layout(text: String): SegLayout {
    val polys = mutableListOf<SegPoly>()
    var x = 0f
    var prevDigit = false
    for (ch in text) {
        when {
            ch.isDigit() -> {
                if (prevDigit) x += Seg.GAP
                val bits = Seg.DIGITS[ch - '0']
                Seg.SHAPES.forEachIndexed { i, shape ->
                    val p = shape.copyOf()
                    for (k in p.indices step 2) p[k] += x
                    polys += SegPoly(p, bits and (1 shl i) != 0)
                }
                x += Seg.W
                prevDigit = true
            }
            ch == ',' || ch == '.' -> {
                x += 0.8f
                val y = Seg.H - Seg.T
                polys += SegPoly(floatArrayOf(x, y, x + Seg.T, y, x + Seg.T, Seg.H, x, Seg.H), true)
                x += Seg.T + 0.8f
                prevDigit = false
            }
        }
    }
    // Inclinación: cuanto más arriba, más a la derecha.
    for (poly in polys) {
        val p = poly.points
        for (k in p.indices step 2) p[k] += (Seg.H - p[k + 1]) * Seg.SKEW
    }
    return SegLayout(x + Seg.H * Seg.SKEW, polys)
}

/** Ancho en píxeles de [text] en 7 segmentos con dígitos de [digitHeight] px. */
fun segmentsWidth(text: String, digitHeight: Float): Float = layout(text).width * digitHeight / Seg.H

/**
 * Dibuja [text] en 7 segmentos sobre un Canvas de Android (marcadores del mapa), con la
 * esquina superior izquierda en ([x], [y]) y dígitos de [digitHeight] px.
 */
fun drawSegments(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, digitHeight: Float) {
    val s = digitHeight / Seg.H
    val on = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = SegmentOn.toArgb() }
    val off = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = SegmentOff.toArgb() }
    for (poly in layout(text).polys) {
        val p = poly.points
        val path = android.graphics.Path().apply {
            moveTo(x + p[0] * s, y + p[1] * s)
            for (k in 2 until p.size step 2) lineTo(x + p[k] * s, y + p[k + 1] * s)
            close()
        }
        canvas.drawPath(path, if (poly.on) on else off)
    }
}

/**
 * Dibuja [text] en 7 segmentos con altura de dígito [digitHeight]. Solo dibuja: quien
 * la usa pone la descripción para TalkBack.
 */
@Composable
fun SegmentDigits(
    text: String,
    digitHeight: Dp,
    modifier: Modifier = Modifier,
    on: Color = SegmentOn,
    off: Color = SegmentOff,
) {
    val lay = remember(text) { layout(text) }
    Canvas(modifier.size(digitHeight * (lay.width / Seg.H), digitHeight)) {
        val s = size.height / Seg.H
        for (poly in lay.polys) {
            val p = poly.points
            val path = Path().apply {
                moveTo(p[0] * s, p[1] * s)
                for (k in 2 until p.size step 2) lineTo(p[k] * s, p[k + 1] * s)
                close()
            }
            drawPath(path, if (poly.on) on else off)
        }
    }
}
