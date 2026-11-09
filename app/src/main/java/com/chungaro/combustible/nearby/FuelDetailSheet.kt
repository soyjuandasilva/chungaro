package com.chungaro.combustible.nearby

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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

/** Panel inferior con el detalle de un combustible: cercana vs barata, ahorro y ranking. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelDetailDialog(
    fuel: FuelType,
    result: NearbyResult,
    radiusKm: Int,
    tankLiters: Double,
    consumptionL100: Double,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHeader(fuel, result, radiusKm)

            val cheapest = result.cheapest
            val nearest = result.nearest
            when {
                cheapest == null && nearest == null -> Text("No hay datos de precios para este combustible.")
                cheapest == null -> {
                    Text(
                        "No hay gasolineras con precio a menos de $radiusKm km. La más cercana:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    StationCard("📍 Más cercana", nearest!!, context, best = false, fuel = fuel, modifier = Modifier.fillMaxWidth())
                }
                nearest == null || nearest.station.id == cheapest.station.id -> {
                    StationCard("⭐ La más cercana y la más barata", cheapest, context, best = true, fuel = fuel, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StationCard("📍 Más cercana", nearest, context, best = false, fuel = fuel, modifier = Modifier.weight(1f).fillMaxHeight())
                        StationCard("💰 Más barata", cheapest, context, best = true, fuel = fuel, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    SavingsBanner(nearest, cheapest, tankLiters, consumptionL100)
                }
            }

            if (result.cheapestList.size > 1) {
                Ranking(result, context)
            }

            Text(
                "Precios oficiales DGEG. Distancias en línea recta desde tu ubicación.",
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
                    "${result.countInRadius} gasolineras a ≤ $radiusKm km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            result.averageInRadius?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Precio medio", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f))
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (best) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = if (best) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    String.format(Locale.getDefault(), "%.3f", s.station.price),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(" €/L", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(
                stationTitle(s.station),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = fuel.uiColor, modifier = Modifier.size(16.dp))
                Text(
                    " " + listOf(formatKm(s.km), s.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (s.station.updated.isNotBlank()) {
                Text("Act. ${s.station.updated}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            if (best) {
                Button(onClick = { openMaps(context, s.station) }, modifier = Modifier.fillMaxWidth()) { Text("Ir") }
            } else {
                OutlinedButton(onClick = { openMaps(context, s.station) }, modifier = Modifier.fillMaxWidth()) { Text("Ir") }
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
                        if (good) "Merece la pena ir a la más barata" else "Mejor quédate con la más cercana",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Diferencia: ${formatEuro(diff)}/L",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (advice != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Ahorro en ${tankLiters.toInt()} L", formatMoney(advice.savings), Modifier.weight(1f))
                    MiniStat("Coste del desvío", "−" + formatMoney(advice.tripCost), Modifier.weight(1f))
                    MiniStat(
                        "Balance",
                        (if (advice.net >= 0) "+" else "−") + formatMoney(kotlin.math.abs(advice.net)),
                        Modifier.weight(1f),
                        valueColor = if (good) GoodGreen else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "Desvío estimado: ${formatKm(advice.extraKm)} ida y vuelta con un consumo de " +
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
private fun Ranking(result: NearbyResult, context: Context) {
    val list = result.cheapestList
    val best = list.first().station.price
    val worst = list.last().station.price
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ranking de la zona", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        list.forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            RankRow(i, s, best, worst, onClick = { openMaps(context, s.station) })
        }
        Text(
            "Toca una gasolinera para ir con Maps.",
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
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(stationTitle(s.station), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                if (index == 0) "mejor" else "+" + String.format(Locale.getDefault(), "%.3f", diff),
                style = MaterialTheme.typography.labelSmall,
                color = if (index == 0) GoodGreen else MaterialTheme.colorScheme.error,
            )
        }
    }
}
