package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.Atesta
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    val verdict = rememberMondayVerdict(vm, fuel, today)
    val zone by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    val zonePct = remember(zone) { computeWeekly(zone, emptyList()).changePct }

    val (icon, title, color) = when (verdict.kind) {
        MondayKind.FILL_NOW -> Triple(Icons.Outlined.LocalGasStation, if (days == 1) "Abastece hoje" else "Abastece antes de segunda", Atesta.colors.warning)
        MondayKind.WAIT -> Triple(Icons.Outlined.Schedule, "Se puderes, espera por segunda", Atesta.colors.down)
        MondayKind.UNCLEAR -> Triple(Icons.AutoMirrored.Outlined.HelpOutline, "Sem previsão clara para segunda", MaterialTheme.colorScheme.onSurfaceVariant)
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
        shape = CardShape,
        color = color.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    Text(
                        "${fuel.short} · segunda-feira, ${nextMonday.format(mondayFormat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            verdict.evidence?.let { n ->
                Surface(
                    onClick = { openLink(context, n.link) },
                    shape = RoundedCornerShape(12.dp),
                    color = Atesta.colors.card,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Outlined.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Column(Modifier.padding(start = 8.dp)) {
                            Text("«${n.title}»", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(
                                n.source + (if (verdict.upVotes + verdict.downVotes > 1) " · ${verdict.upVotes + verdict.downVotes} notícias esta semana" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
    val v = String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.1f%%", abs(pct))
    return when {
        pct > 0.05 -> "o preço subiu $v"
        pct < -0.05 -> "o preço desceu $v"
        else -> "o preço está estável"
    }
}

/**
 * Veredicto para el próximo lunes. Solo cuentan las noticias publicadas desde
 * el martes anterior a ese lunes: las del propio lunes («ficam mais baratos
 * hoje») hablan del cambio que ya ocurrió.
 */
@Composable
fun rememberMondayVerdict(vm: NearbyViewModel, fuel: FuelType, today: LocalDate = LocalDate.now()): MondayVerdict {
    val since = remember(today) { predictionWindowStart(today) }
    return remember(vm.news, fuel.id, since) {
        mondayVerdict(vm.news.filter { (it.publishedAt ?: 0L) >= since }, familyOf(fuel.id))
    }
}

/** Inicio (martes 00:00, hora de Lisboa) de la ventana de noticias que hablan del próximo lunes. */
fun predictionWindowStart(today: LocalDate): Long =
    today.plusDays(daysToMonday(today).toLong() - 6).atStartOfDay(ZoneId.of("Europe/Lisbon")).toInstant().toEpochMilli()
