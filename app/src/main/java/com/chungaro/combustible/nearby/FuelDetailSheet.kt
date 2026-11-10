package com.chungaro.combustible.nearby

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

private val Gold = Color(0xFFEAB308)
private val Silver = Color(0xFF94A3B8)
private val Bronze = Color(0xFFB45309)
private val GoodGreen = Color(0xFF16A34A)

/** Panel inferior con el detalle de un combustible: cercana vs barata, ahorro, tendencia y ranking. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelDetailDialog(
    vm: NearbyViewModel,
    fuel: FuelType,
    result: NearbyResult,
    radiusKm: Int,
    tankLiters: Double,
    consumptionL100: Double,
    onOpenStation: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(fuel, result, radiusKm)

            val cheapest = result.cheapest
            val nearest = result.nearest
            when {
                cheapest == null && nearest == null -> Text("Não há dados de preços para este combustível.")
                cheapest == null -> {
                    Text(
                        "Não há bombas com preço a menos de $radiusKm km. A mais próxima:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    StationCard("📍 Mais próxima", nearest!!, context, best = false, fuel = fuel, onOpen = onOpenStation)
                }
                nearest == null || nearest.station.id == cheapest.station.id -> {
                    StationCard("⭐ A mais próxima e a mais barata", cheapest, context, best = true, fuel = fuel, onOpen = onOpenStation)
                }
                else -> {
                    StationCard("💰 Mais barata", cheapest, context, best = true, fuel = fuel, onOpen = onOpenStation)
                    StationCard("📍 Mais próxima", nearest, context, best = false, fuel = fuel, onOpen = onOpenStation)
                    SavingsBanner(nearest, cheapest, tankLiters, consumptionL100)
                }
            }

            TrendCard("Tendência na zona (média a ≤ ${HistoryRecorder.ZONE_KM.toInt()} km)", zone, fuel.uiColor)

            if (result.cheapestList.size > 1) {
                Ranking(result, onOpenStation)
            }

            Text(
                "Preços oficiais da DGEG. Distâncias em linha reta desde a sua localização.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SheetHeader(fuel: FuelType, result: NearbyResult, radiusKm: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(fuel.uiColor, fuel.uiColor.copy(alpha = 0.65f))))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fuel.short, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${result.countInRadius} bombas a ≤ $radiusKm km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            result.averageInRadius?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Preço médio", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
                    Text(formatEuro(it), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StationCard(
    label: String,
    s: StationAtDistance,
    context: Context,
    best: Boolean,
    fuel: FuelType,
    onOpen: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(s.station.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (best) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = if (best) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(s.station.brand, size = 46.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stationTitle(s.station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = fuel.uiColor, modifier = Modifier.size(14.dp))
                    Text(
                        " " + listOf(formatKm(s.km), s.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format(Locale.getDefault(), "%.3f", s.station.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(" €", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 3.dp))
                }
                if (best) {
                    Button(onClick = { openMaps(context, s.station) }, contentPadding = PaddingValues(horizontal = 20.dp)) { Text("Ir") }
                } else {
                    OutlinedButton(onClick = { openMaps(context, s.station) }, contentPadding = PaddingValues(horizontal = 20.dp)) { Text("Ir") }
                }
            }
        }
    }
}

@Composable
private fun SavingsBanner(nearest: StationAtDistance, cheapest: StationAtDistance, tankLiters: Double, consumptionL100: Double) {
    val diff = nearest.station.price - cheapest.station.price
    val advice = tripAdvice(nearest, cheapest, tankLiters, consumptionL100)
    val good = advice?.worthIt == true
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (good) GoodGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (good) "✅" else "✋", style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        if (good) "Compensa ir à mais barata" else "Mais vale ficar pela mais próxima",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Diferença: ${formatEuro(diff)}/L",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (advice != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Poupança em ${tankLiters.toInt()} L", formatMoney(advice.savings), Modifier.weight(1f))
                    MiniStat("Custo do desvio", "−" + formatMoney(advice.tripCost), Modifier.weight(1f))
                    MiniStat(
                        "Balance",
                        (if (advice.net >= 0) "+" else "−") + formatMoney(kotlin.math.abs(advice.net)),
                        Modifier.weight(1f),
                        valueColor = if (good) GoodGreen else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "Desvio estimado: ${formatKm(advice.extraKm)} ida e volta com um consumo de " +
                        String.format(Locale.getDefault(), "%.1f", consumptionL100) + " L/100 km.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier, valueColor: Color = Color.Unspecified) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun Ranking(result: NearbyResult, onOpen: (Long) -> Unit) {
    val list = result.cheapestList
    val best = list.first().station.price
    val worst = list.last().station.price
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ranking da zona", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        list.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            RankRow(i, s, best, worst, onClick = { onOpen(s.station.id) })
        }
        Text(
            "Toque numa bomba para ver a ficha e a tendência.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankRow(index: Int, s: StationAtDistance, best: Double, worst: Double, onClick: () -> Unit) {
    val medal = when (index) {
        0 -> Gold
        1 -> Silver
        2 -> Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val diff = s.station.price - best
    // 1 = la más barata, 0 = la más cara del ranking.
    val score = if (worst - best < 1e-6) 1f else (1 - (s.station.price - best) / (worst - best)).toFloat()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(medal), contentAlignment = Alignment.Center) {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (index < 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BrandBadge(s.station.brand, size = 30.dp, modifier = Modifier.padding(start = 8.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(stationTitle(s.station), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatKm(s.km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.15f + 0.85f * score)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (index == 0) GoodGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatEuro(s.station.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                if (index == 0) "melhor" else "+" + String.format(Locale.getDefault(), "%.3f", diff),
                style = MaterialTheme.typography.labelSmall,
                color = if (index == 0) GoodGreen else MaterialTheme.colorScheme.error,
            )
        }
    }
}
