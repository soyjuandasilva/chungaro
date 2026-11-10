package com.chungaro.combustible.nearby

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/** Litros usados para mostrar cuánto se ahorra en un depósito. */
const val TANK_LITERS = 50

val FuelType.uiColor: Color get() = Color(color)

@Composable
fun FuelPill(fuel: FuelType, modifier: Modifier = Modifier) {
    Surface(
        color = fuel.uiColor.copy(alpha = 0.15f),
        contentColor = fuel.uiColor,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(fuel.uiColor, CircleShape))
            Text(
                "  " + fuel.short,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun StationBlock(s: StationAtDistance, context: Context, accent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stationTitle(s.station), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(formatKm(s.km), s.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${formatEuro(s.station.price)}/L",
                style = if (accent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = { openMaps(context, s.station) }) { Text("Como chegar") }
        }
    }
}

fun comparison(nearest: StationAtDistance, cheapest: StationAtDistance, liters: Double = TANK_LITERS.toDouble()): String {
    if (nearest.station.id == cheapest.station.id) return "A mais próxima é também a mais barata. Perfeito!"
    val diff = nearest.station.price - cheapest.station.price
    if (abs(diff) < 0.0005) return "A mais próxima tem o mesmo preço que a mais barata."
    return if (diff > 0) {
        "A mais próxima custa mais ${formatEuro(diff)}/L: em ${liters.toInt()} L pagaria mais ${formatMoney(diff * liters)}. " +
            "A mais barata fica ${formatKm(cheapest.km - nearest.km)} mais longe."
    } else {
        "A mais próxima é ${formatEuro(-diff)}/L mais barata que a melhor dentro do raio."
    }
}

@Composable
fun TripAdviceBox(a: TripAdvice) {
    val good = a.worthIt
    Surface(
        color = if (good) Color(0xFF16A34A).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (good) "✅ Compensa ir à mais barata" else "✋ O desvio não compensa",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Poupa ${formatMoney(a.savings)} no depósito; o desvio (~${formatKm(a.extraKm)} ida e volta) " +
                    "gasta cerca de ${formatMoney(a.tripCost)} de combustível. " +
                    if (good) "Ganha ${formatMoney(a.net)}." else "Perderia ${formatMoney(-a.net)}.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

fun stationTitle(s: PricedStation): String =
    // La insignia ya muestra la marca: aquí solo el nombre de la bomba.
    s.name.ifBlank { s.brand }.ifBlank { "Bomba ${s.id}" }

fun formatEuro(v: Double): String = String.format(Locale.getDefault(), "%.3f €", v)
fun formatMoney(v: Double): String = String.format(Locale.getDefault(), "%.2f €", v)
fun formatKm(v: Double): String =
    if (v < 1) String.format(Locale.getDefault(), "%.0f m", v * 1000)
    else String.format(Locale.getDefault(), "%.1f km", v)

fun openMaps(context: Context, s: PricedStation) {
    val label = Uri.encode(stationTitle(s))
    val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}($label)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${s.lat},${s.lon}")),
        )
    }
}
