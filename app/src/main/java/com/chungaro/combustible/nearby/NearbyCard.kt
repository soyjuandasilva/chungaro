package com.chungaro.combustible.nearby

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/** Litros usados para mostrar cuánto se ahorra en un depósito. */
private const val TANK_LITERS = 50

@Composable
fun NearbyCard(vm: NearbyViewModel) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission && vm.state is NearbyState.Idle) vm.refresh()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Text(
                    "  Cerca de ti",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (hasPermission) {
                    IconButton(onClick = { vm.refresh(force = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            }

            if (!hasPermission) {
                Text(
                    "Activa tu ubicación para ver la gasolinera más barata y la más cercana, con precios oficiales de la DGEG.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = {
                    launcher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }) { Text("Usar mi ubicación") }
                return@Column
            }

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FUEL_TYPES.forEach { f ->
                    FilterChip(
                        selected = f.id == vm.fuelId,
                        onClick = { vm.selectFuel(f.id) },
                        label = { Text(f.name) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Radio:", style = MaterialTheme.typography.labelLarge)
                NearbyViewModel.RADIUS_OPTIONS.forEach { km ->
                    FilterChip(
                        selected = km == vm.radiusKm,
                        onClick = { vm.selectRadius(km) },
                        label = { Text("$km km") },
                    )
                }
            }

            when (val s = vm.state) {
                NearbyState.Idle, NearbyState.Loading -> Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is NearbyState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.refresh(force = true) }) { Text("Reintentar") }
                }

                is NearbyState.Ready -> NearbyResultView(s.result, vm.radiusKm, context)
            }
        }
    }
}

@Composable
private fun NearbyResultView(r: NearbyResult, radiusKm: Int, context: Context) {
    val cheapest = r.cheapest
    val nearest = r.nearest

    if (cheapest == null) {
        Text("No hay gasolineras con precio a menos de $radiusKm km. Prueba con un radio mayor.")
    } else {
        Text("Más barata a menos de $radiusKm km", style = MaterialTheme.typography.labelLarge)
        StationBlock(cheapest, context, highlight = true)
    }

    if (nearest != null) {
        HorizontalDivider()
        Text("Más cercana", style = MaterialTheme.typography.labelLarge)
        StationBlock(nearest, context, highlight = false)
        if (cheapest != null) {
            Text(comparison(nearest, cheapest), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }

    if (r.cheapestList.size > 1) {
        HorizontalDivider()
        Text("Más baratas de la zona (${r.countInRadius} gasolineras)", style = MaterialTheme.typography.labelLarge)
        r.cheapestList.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${i + 1}. ${title(s.station)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${km(s.km)} · ${euro(s.station.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        r.averageInRadius?.let {
            Text(
                "Precio medio en la zona: ${euro(it)}/L",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StationBlock(s: StationAtDistance, context: Context, highlight: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title(s.station), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(km(s.km), s.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${euro(s.station.price)}/L",
                style = if (highlight) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = { openMaps(context, s.station) }) { Text("Cómo llegar") }
        }
    }
}

private fun comparison(nearest: StationAtDistance, cheapest: StationAtDistance): String {
    if (nearest.station.id == cheapest.station.id) return "La más cercana es también la más barata."
    val diff = nearest.station.price - cheapest.station.price
    if (abs(diff) < 0.0005) return "La más cercana tiene el mismo precio que la más barata."
    val extraKm = cheapest.km - nearest.km
    val tank = diff * TANK_LITERS
    return if (diff > 0) {
        "La más cercana cuesta ${euro(diff)}/L más: en $TANK_LITERS L pagarías ${money(tank)} más. " +
            "La más barata está ${km(extraKm)} más lejos."
    } else {
        // Puede pasar si la más cercana está fuera del radio elegido.
        "La más cercana es ${euro(-diff)}/L más barata que la mejor dentro del radio."
    }
}

private fun title(s: PricedStation): String =
    listOf(s.brand, s.name).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifBlank { "Gasolinera ${s.id}" }

private fun euro(v: Double) = String.format(Locale.getDefault(), "%.3f €", v)
private fun money(v: Double) = String.format(Locale.getDefault(), "%.2f €", v)
private fun km(v: Double) =
    if (v < 1) String.format(Locale.getDefault(), "%.0f m", v * 1000) else String.format(Locale.getDefault(), "%.1f km", v)

private fun openMaps(context: Context, s: PricedStation) {
    val label = Uri.encode(title(s))
    val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}($label)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${s.lat},${s.lon}")),
        )
    }
}
