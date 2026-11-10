package com.chungaro.combustible.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chungaro.combustible.ui.APP_LOCALE
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Amber = Color(0xFFD97706)
private val Green = Color(0xFF15803D)
private val mondayFormat = DateTimeFormatter.ofPattern("d/MM", APP_LOCALE)

/**
 * «Abastece hoje ou espera por segunda?»: veredicto para [fuel] según las
 * noticias de esta semana sobre los precios del lunes.
 */
@Composable
fun MondayCard(vm: NearbyViewModel, fuel: FuelType, onOpenNews: (() -> Unit)? = null) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.loadNews() }

    val today = LocalDate.now()
    val days = daysToMonday(today)
    val nextMonday = today.plusDays(days.toLong())
    // Solo las noticias publicadas desde el lunes de esta semana hablan del próximo lunes.
    val weekStart = remember(today) {
        today.minusDays((7 - days).toLong() % 7).atStartOfDay(ZoneId.of("Europe/Lisbon")).toInstant().toEpochMilli()
    }
    val verdict = remember(vm.news, fuel.id, weekStart) {
        mondayVerdict(vm.news.filter { (it.publishedAt ?: 0L) >= weekStart }, familyOf(fuel.id))
    }
    val zone by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    val zonePct = remember(zone) { computeWeekly(zone, emptyList()).changePct }

    val (emoji, title, color) = when (verdict.kind) {
        MondayKind.FILL_NOW -> Triple("⛽", if (days == 1) "Abastece hoje" else "Abastece antes de segunda", Amber)
        MondayKind.WAIT -> Triple("⏳", "Se puderes, espera por segunda", Green)
        MondayKind.UNCLEAR -> Triple("🤔", "Sem previsão clara para segunda", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val body = when (verdict.kind) {
        MondayKind.FILL_NOW -> "As notícias apontam para uma subida do ${fuel.short} na segunda-feira" +
            (if (days == 1) ": amanhã já pode estar mais caro." else ". Enche o depósito esta semana.")
        MondayKind.WAIT -> "As notícias apontam para uma descida do ${fuel.short} na segunda-feira. " +
            "Se tiveres combustível para aguentar, abastece só o necessário até lá."
        MondayKind.UNCLEAR -> when {
            vm.news.isEmpty() && vm.newsLoading -> "A ler as notícias da semana…"
            days >= 5 -> "As previsões para a próxima semana costumam sair à quinta e à sexta-feira."
            else -> "Não há notícias claras sobre o ${fuel.short}."
        } + (zonePct?.let { " Na tua zona, " + trendText(it) + " face à semana passada." } ?: "")
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "${fuel.short.uppercase()} · SEGUNDA ${nextMonday.format(mondayFormat)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            verdict.evidence?.let { n ->
                Surface(
                    onClick = { openLink(context, n.link) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📰 «${n.title}»", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(
                            n.source + (if (verdict.upVotes + verdict.downVotes > 1) " · ${verdict.upVotes + verdict.downVotes} notícias esta semana" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            onOpenNews?.let {
                TextButton(onClick = it, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Ver as notícias da semana", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun trendText(pct: Double): String {
    val v = String.format(Locale.getDefault(), "%.1f%%", abs(pct))
    return when {
        pct > 0.05 -> "o preço subiu $v"
        pct < -0.05 -> "o preço desceu $v"
        else -> "o preço está estável"
    }
}
