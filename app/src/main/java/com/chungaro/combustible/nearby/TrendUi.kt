package com.chungaro.combustible.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.PricePoint
import com.chungaro.combustible.ui.APP_LOCALE
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val dayFormat = DateTimeFormatter.ofPattern("d MMM", APP_LOCALE)
private val Up = Color(0xFFDC2626)
private val Down = Color(0xFF16A34A)

/** Tarjeta "Tendência do preço": precio, variación, gráfico y recomendación. */
@Composable
fun TrendCard(title: String, points: List<PricePoint>, color: Color) {
    val trend = computeTrend(points)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color, shape = RoundedCornerShape(2.dp), modifier = Modifier.width(4.dp).height(20.dp)) {}
                Text(
                    "  $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (trend.last != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatEuro(trend.last), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    trend.changePct?.let { pct ->
                        val up = pct > 0.05
                        val down = pct < -0.05
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                up -> Up.copy(alpha = 0.12f)
                                down -> Down.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Text(
                                (if (up) "↗ +" else if (down) "↘ " else "→ ") +
                                    String.format(Locale.getDefault(), "%.1f%%", pct),
                                color = when {
                                    up -> Up
                                    down -> Down
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            if (points.size >= 2) {
                TrendChart(points, color)
                Row {
                    Text(
                        LocalDate.ofEpochDay(points.first().day).format(dayFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (points.last().day == LocalDate.now().toEpochDay()) "Hoje"
                        else LocalDate.ofEpochDay(points.last().day).format(dayFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AdviceBanner(trend)
        }
    }
}

@Composable
private fun TrendChart(points: List<PricePoint>, color: Color) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = points.map { it.price }
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 1e-6 } ?: 0.01
    val firstDay = points.first().day
    val days = (points.last().day - firstDay).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 6.dp)) {
        listOf(0f, 0.5f, 1f).forEach { f ->
            drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1f)
        }
        fun pt(p: PricePoint) = Offset(
            ((p.day - firstDay).toFloat() / days) * size.width,
            (size.height - (p.price - min) / span * size.height * 0.9 - size.height * 0.05).toFloat(),
        )
        val line = Path().apply {
            moveTo(pt(points.first()).x, pt(points.first()).y)
            points.drop(1).forEach { lineTo(pt(it).x, pt(it).y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pt(points.last()).x, size.height)
            lineTo(pt(points.first()).x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
        val last = pt(points.last())
        drawCircle(Color.White, radius = 9f, center = last)
        drawCircle(color, radius = 6f, center = last)
    }
}

@Composable
fun AdviceBanner(trend: Trend) {
    val pct = trend.changePct?.let { String.format(Locale.getDefault(), "%.1f%%", abs(it)) } ?: ""
    val (emoji, title, sub) = when (trend.advice) {
        Advice.COLLECTING -> Triple(
            "📊",
            "A recolher histórico",
            "Faltam ${(MIN_DAYS_FOR_TREND - trend.days).coerceAtLeast(1)} dia(s) para ver a tendência. A app guarda os preços sempre que os consulta.",
        )
        Advice.LOWEST -> Triple("😍", "Preço mais baixo dos últimos 14 dias!", "Bom momento para abastecer.")
        Advice.BUY_NOW_RISING -> Triple("😨", "Mais vale abastecer hoje!", "O preço subiu $pct na última semana.")
        Advice.WAIT_FALLING -> Triple("😎", "Pode esperar", "O preço desceu $pct na última semana.")
        Advice.STABLE -> Triple("😐", "Preço estável", "Sem grandes mudanças na última semana.")
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                Text(sub, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
