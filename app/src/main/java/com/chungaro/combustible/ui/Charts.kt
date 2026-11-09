package com.chungaro.combustible.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Stats
import com.chungaro.combustible.data.pricePerUnit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ChartsSection(stats: Stats, settings: AppSettings) {
    val refuels = stats.rows.map { it.refuel }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (refuels.isEmpty()) {
            Text(
                "Registe alguns abastecimentos para ver os seus gráficos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            )
            return@Column
        }

        // Gasto por mes (últimos 12 meses).
        val now = YearMonth.now()
        val months = (11 downTo 0).map { now.minusMonths(it.toLong()) }
        val byMonth = refuels.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }
            .mapValues { (_, list) -> list.sumOf { it.totalCost } }
        val monthly = months.map { byMonth[it] ?: 0.0 }
        val active = monthly.filter { it > 0 }
        ChartCard(
            title = "Gasto por mês",
            subtitle = "Últimos 12 meses: ${settings.money(monthly.sum())}" +
                (if (active.isNotEmpty()) " · média ${settings.money(active.average())}/mês" else ""),
        ) {
            BarChart(
                values = monthly,
                labels = months.map { it.month.getDisplayName(TextStyle.NARROW, APP_LOCALE) },
            )
        }

        // Precio pagado por unidad.
        val prices = refuels.takeLast(24).map { it.pricePerUnit() }
        if (prices.size >= 2) {
            ChartCard(
                title = "Preço por ${settings.unitLabel}",
                subtitle = "Último ${settings.moneyPrecise(prices.last())} · mín ${settings.moneyPrecise(prices.min())} · máx ${settings.moneyPrecise(prices.max())}",
            ) { LineChart(prices, MaterialTheme.colorScheme.secondary) }
        }

        // Consumo por tramo de tanque lleno.
        val consumption = stats.rows.mapNotNull { it.efficiency }.takeLast(24).map { settings.consumptionValue(it) }
        if (consumption.size >= 2) {
            ChartCard(
                title = "Consumo (${settings.consumptionLabel})",
                subtitle = "Último ${formatNumber(consumption.last(), 1)} · média ${formatNumber(consumption.average(), 1)}",
            ) { LineChart(consumption, MaterialTheme.colorScheme.tertiary) }
        } else {
            Text(
                "O gráfico de consumo aparece com pelo menos três abastecimentos de depósito cheio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun BarChart(values: List<Double>, labels: List<String>) {
    val color = MaterialTheme.colorScheme.primary
    val last = MaterialTheme.colorScheme.secondary
    val empty = MaterialTheme.colorScheme.outlineVariant
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val slot = size.width / values.size
        val barW = slot * 0.62f
        values.forEachIndexed { i, v ->
            val h = if (v > 0) (v / max * size.height).toFloat().coerceAtLeast(4f) else 3f
            drawRoundRect(
                color = when {
                    v <= 0 -> empty
                    i == values.lastIndex -> last
                    else -> color
                },
                topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
    Row(Modifier.fillMaxWidth()) {
        labels.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LineChart(values: List<Double>, color: Color) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 1e-9 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
        listOf(0f, 0.5f, 1f).forEach { f ->
            drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1f)
        }
        val stepX = size.width / (values.size - 1)
        fun point(i: Int) = Offset(i * stepX, (size.height - (values[i] - min) / span * size.height).toFloat())
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until values.size) lineTo(point(i).x, point(i).y)
        }
        drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round))
        values.indices.forEach { drawCircle(color, radius = 7f, center = point(it)) }
    }
}
