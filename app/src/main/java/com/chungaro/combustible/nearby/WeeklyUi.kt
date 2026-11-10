package com.chungaro.combustible.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlin.math.abs

private val Green = Color(0xFF16A34A)
private val Red = Color(0xFFDC2626)

/** "Recomendação da semana": compara esta semana con la anterior en la zona. */
@Composable
fun WeeklyCard(vm: NearbyViewModel, fuel: FuelType, onOpenStation: (Long) -> Unit) {
    val zone by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    val stations by remember(fuel.id) { vm.stationsHistory(fuel.id) }.collectAsStateWithLifecycle(emptyList())
    val w = remember(zone, stations) { computeWeekly(zone, stations) }
    val names = vm.stationsOf(fuel.id)?.associateBy { it.id }.orEmpty()
    val pct = w.changePct?.let { String.format(Locale.getDefault(), "%.1f%%", abs(it)) } ?: ""

    val (emoji, title, body, tint) = when (w.advice) {
        WeeklyAdvice.COLLECTING -> Quad(
            "📊",
            "A recolher histórico",
            "A app guarda automaticamente, várias vezes por dia e mesmo fechada, o preço de cada combustível em cada bomba da tua zona. " +
                "A comparação semanal aparece quando houver uma semana de dados (dia ${w.days.coerceAtLeast(1)} de $WEEK_DAYS).",
            MaterialTheme.colorScheme.primary,
        )
        WeeklyAdvice.FILL_THIS_WEEK -> Quad(
            "📉",
            "Boa semana para abastecer",
            "O ${fuel.short} está $pct mais barato do que na semana passada na tua zona " +
                "(média ${formatEuro(w.zoneBefore!!)} → ${formatEuro(w.zoneNow!!)}).",
            Green,
        )
        WeeklyAdvice.WAIT -> Quad(
            "📈",
            "Se puderes, adia o abastecimento",
            "O ${fuel.short} subiu $pct face à semana passada " +
                "(média ${formatEuro(w.zoneBefore!!)} → ${formatEuro(w.zoneNow!!)}). Abastece só o necessário.",
            Red,
        )
        WeeklyAdvice.STABLE -> Quad(
            "➖",
            "Preços estáveis esta semana",
            "O ${fuel.short} variou só $pct face à semana passada (média ${formatEuro(w.zoneNow!!)}). " +
                "Abastece quando precisares, de preferência na mais barata.",
            MaterialTheme.colorScheme.primary,
        )
    }

    Surface(shape = RoundedCornerShape(20.dp), color = tint.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.padding(start = 10.dp)) {
                    Text("${fuel.short.uppercase()} · ESTA SEMANA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = tint)
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (w.advice == WeeklyAdvice.COLLECTING) {
                LinearProgressIndicator(
                    progress = { (w.days.toFloat() / WEEK_DAYS).coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (w.dropped.isNotEmpty()) {
                Text("Baixaram o preço", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Green)
                w.dropped.forEach { ChangeRow(it, names[it.stationId], onOpenStation) }
            }
            if (w.risen.isNotEmpty()) {
                Text("Subiram o preço", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Red)
                w.risen.forEach { ChangeRow(it, names[it.stationId], onOpenStation) }
            }
        }
    }
}

@Composable
private fun ChangeRow(c: StationChange, station: PricedStation?, onOpen: (Long) -> Unit) {
    val up = c.diff > 0
    Surface(
        onClick = { onOpen(c.stationId) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(station?.brand ?: "", size = 28.dp)
            Text(
                station?.let { stationTitle(it) } ?: "Bomba ${c.stationId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(formatEuro(c.now), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    (if (up) "+" else "−") + String.format(Locale.getDefault(), "%.3f", abs(c.diff)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (up) Red else Green,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: Color)
