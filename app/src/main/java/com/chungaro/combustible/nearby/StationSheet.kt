package com.chungaro.combustible.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chungaro.combustible.ui.formatDate
import java.time.LocalDate
import java.util.Locale

/** Fondo oscuro de las cabeceras (el verde oscuro de Atesta). */
val HeaderDark = com.chungaro.combustible.ui.AtestaGreen

/**
 * Ficha de una bomba: cabecera oscura con logotipo, favorita y «Ir lá»;
 * precios de todos sus combustibles, tendencia y horario.
 * [isFavorite] null oculta el botón de favorita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSheet(
    vm: NearbyViewModel,
    stationId: Long,
    defaultFuelId: Int,
    onDismiss: () -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: () -> Unit = {},
) {
    val context = LocalContext.current
    val entries = FUEL_TYPES.mapNotNull { f -> vm.stationsOf(f.id)?.firstOrNull { it.id == stationId }?.let { f to it } }
    val base = entries.firstOrNull()?.second
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = null,
    ) {
        if (base == null) {
            Text("Sem dados para esta bomba.", Modifier.padding(24.dp))
            return@ModalBottomSheet
        }
        var selFuel by rememberSaveable(stationId) {
            mutableIntStateOf(entries.firstOrNull { it.first.id == defaultFuelId }?.first?.id ?: entries.first().first.id)
        }
        val selected = FUEL_TYPES.first { it.id == selFuel }
        val km = vm.location?.let { (lat, lon) -> distanceKm(lat, lon, base.lat, base.lon) }
        val points by remember(selFuel, stationId) { vm.history(selFuel, stationId) }.collectAsStateWithLifecycle(emptyList())

        // Cabecera oscura
        Surface(color = HeaderDark, contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                // Al subir la hoja del todo, deja sitio a la barra de estado.
                SheetStatusBarSpacer(state)
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.4f), modifier = Modifier.width(40.dp).height(5.dp)) {}
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) { BrandBadge(base.brand, size = 50.dp) }
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(stationTitle(base), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        // Dirección completa en su propia línea (hasta 2), sin cortarse.
                        Text(
                            listOf(base.address, base.locality).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Distancia, favorita y «Ir lá» en su propia fila para dar sitio al texto.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        km?.let { "➤ " + formatKm(it) + " de ti" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                    )
                    isFavorite?.let { fav ->
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (fav) "Tirar das favoritas" else "Guardar nas favoritas",
                                tint = if (fav) Color(0xFFF87171) else Color.White,
                            )
                        }
                    }
                    Button(
                        onClick = { openMaps(context, base) },
                        colors = ButtonDefaults.buttonColors(containerColor = com.chungaro.combustible.ui.AtestaGreenLight, contentColor = HeaderDark),
                    ) { Text("Ir lá", fontWeight = FontWeight.Bold) }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BlockTitle("Combustíveis a ${formatDate(LocalDate.now().toEpochDay())}")
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 2.dp) {
                Column {
                    entries.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (fuel, st) ->
                                FuelTile(fuel, st, selected = fuel.id == selFuel, modifier = Modifier.weight(1f)) { selFuel = fuel.id }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
                            color = if (d <= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            BlockTitle("Tendência de preços")
            TrendCard(selected.short, points, selected.uiColor)

            Spacer(Modifier.height(8.dp))
            BlockTitle("Morada")
            InfoCard {
                Text(
                    listOf(base.address, base.locality).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { "Sem morada indicada" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { openMaps(context, base) }) { Text("Abrir no mapa") }
            }

            Text(
                "Preços oficiais da DGEG. A app guarda o histórico desde que a começaste a usar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Tarjeta blanca de información (horario, morada). */
@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

/**
 * Hueco que crece cuando la hoja llega arriba del todo, para que la cabecera
 * no quede debajo de la barra de estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetStatusBarSpacer(state: SheetState) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density).toFloat()
    val offset = runCatching { state.requireOffset() }.getOrDefault(Float.MAX_VALUE)
    val extra = (statusTop - offset).coerceIn(0f, statusTop)
    if (extra > 0f) Spacer(Modifier.height(with(density) { extra.toDp() }))
}

@Composable
private fun BlockTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun FuelTile(fuel: FuelType, st: PricedStation, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) fuel.uiColor.copy(alpha = 0.10f) else Color.Transparent,
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(14.dp).background(fuel.uiColor, RoundedCornerShape(2.dp)))
                Text(
                    " " + fuel.short.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    String.format(Locale.getDefault(), "%.3f", st.price),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                )
                Text("€", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (st.updated.isNotBlank()) {
                Text(updatedText(st.updated), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
