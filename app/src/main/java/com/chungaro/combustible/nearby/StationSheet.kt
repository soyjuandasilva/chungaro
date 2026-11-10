package com.chungaro.combustible.nearby

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chungaro.combustible.ui.appBackground
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chungaro.combustible.ui.AtestaBorder
import com.chungaro.combustible.ui.AtestaGreenLight
import com.chungaro.combustible.ui.ChungaroTheme
import kotlin.math.roundToInt

/** Fondo oscuro de las cabeceras. */
val HeaderDark = com.chungaro.combustible.ui.AtestaGreen

/**
 * «Detalhes do posto» a pantalla completa: banner con la marca, nombre,
 * morada, distancia, «Como chegar», precios de todos sus combustibles,
 * tendencia e información. [isFavorite] null oculta el corazón.
 */
@Composable
fun StationSheet(
    vm: NearbyViewModel,
    stationId: Long,
    defaultFuelId: Int,
    onDismiss: () -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ChungaroTheme {
            StationDetail(vm, stationId, defaultFuelId, onDismiss, isFavorite, onToggleFavorite)
        }
    }
}

@Composable
private fun StationDetail(
    vm: NearbyViewModel,
    stationId: Long,
    defaultFuelId: Int,
    onBack: () -> Unit,
    isFavorite: Boolean?,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val entries = FUEL_TYPES.mapNotNull { f -> vm.stationsOf(f.id)?.firstOrNull { it.id == stationId }?.let { f to it } }
    val base = entries.firstOrNull()?.second

    Scaffold(
        modifier = Modifier.appBackground(),
        containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            DarkTopBar("Detalhes do posto", icon = null, onBack = onBack) {
                if (base != null) {
                    IconButton(onClick = {
                        val text = "${stationTitle(base)} — ${listOf(base.address, base.locality).filter { it.isNotBlank() }.joinToString(", ")}\n" +
                            entries.joinToString("\n") { (f, s) -> "${f.short}: ${euro3(s.price)}" } +
                            "\nhttps://maps.google.com/?q=${base.lat},${base.lon}"
                        context.startActivity(
                            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Partilhar posto"),
                        )
                    }) { Icon(Icons.Outlined.Share, contentDescription = "Partilhar") }
                }
                isFavorite?.let { fav ->
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (fav) "Tirar dos favoritos" else "Guardar nos favoritos",
                            tint = if (fav) Color(0xFFEF4444) else Color.White,
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (base == null) {
            Text("Sem dados para este posto.", Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        var selFuel by rememberSaveable(stationId) {
            mutableIntStateOf(entries.firstOrNull { it.first.id == defaultFuelId }?.first?.id ?: entries.first().first.id)
        }
        val selected = FUEL_TYPES.first { it.id == selFuel }
        val km = vm.location?.let { (lat, lon) -> distanceKm(lat, lon, base.lat, base.lon) }
        val points by remember(selFuel, stationId) { vm.history(selFuel, stationId) }.collectAsStateWithLifecycle(emptyList())
        val brandColor = Color(pinColor(base.brand))

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Banner con la marca (la DGEG no tiene fotos de los postos)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.linearGradient(listOf(brandColor, brandColor.copy(alpha = 0.55f), Color(0xFF0E2A1D))),
                        RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(96.dp)) {
                    Box(contentAlignment = Alignment.Center) { BrandBadge(base.brand, size = 84.dp) }
                }
            }

            // Nombre, morada, distancia y «Como chegar»
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandTile(base.brand, size = 56.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stationTitle(base), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            " " + listOf(base.address, base.locality).filter { it.isNotBlank() }.distinct().joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (km != null) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Tiempo aproximado: carretera ≈ 1,3 × línea recta a ~45 km/h de media.
                    val min = (km * 1.3 / 45.0 * 60).roundToInt().coerceAtLeast(1)
                    Text(" ${formatKm(km)} ($min min)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = { openMaps(context, base) },
                    colors = ButtonDefaults.buttonColors(containerColor = AtestaGreenLight, contentColor = Color(0xFF052E16)),
                ) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Como chegar", fontWeight = FontWeight.ExtraBold)
                }
            }

            // Precios de todos sus combustibles
            DarkCard {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Preços de combustíveis", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    // Cuándo comprobó la app los precios en la DGEG (lo hace cada 10 minutos).
                    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(30_000L)
                            now = System.currentTimeMillis()
                        }
                    }
                    vm.pricesTime?.let { t ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Atualizado " + agoText(t, now), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AtestaGreenLight)
                            Text("a cada 10 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                entries.forEachIndexed { i, (fuel, st) ->
                    if (i > 0) HorizontalDivider(color = AtestaBorder)
                    val isSel = fuel.id == selFuel
                    Surface(onClick = { selFuel = fuel.id }, color = if (isSel) fuel.uiColor.copy(alpha = 0.10f) else Color.Transparent) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = fuel.uiColor, modifier = Modifier.size(24.dp))
                            Text("  " + fuel.short, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            PriceChip(st.price, fuel.uiColor, big = true)
                        }
                    }
                }
                // Comparación con la media de la zona del combustible elegido
                val st = entries.first { it.first.id == selFuel }.second
                (vm.rows[selFuel] as? FuelRowState.Ready)?.result?.averageInRadius?.let { avg ->
                    val d = st.price - avg
                    Text(
                        if (d <= 0) "${selected.short}: ${formatEuro(-d)} abaixo da média da zona"
                        else "${selected.short}: ${formatEuro(d)} acima da média da zona",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (d <= 0) AtestaGreenLight else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            // Tendencia del combustible elegido
            TrendCard("Tendência · ${selected.short}", points, selected.uiColor)

            // Información
            DarkCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = AtestaGreenLight)
                        Text("  Informações do posto", fontWeight = FontWeight.ExtraBold)
                    }
                    InfoLine("Morada", listOf(base.address, base.locality).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { "—" })
                    if (base.brand.isNotBlank()) InfoLine("Marca", base.brand)
                    InfoLine("Combustíveis", entries.joinToString(", ") { it.first.short })
                }
            }
            Text(
                "Preços oficiais da DGEG. A app guarda o histórico desde que a começaste a usar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text("$label: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
