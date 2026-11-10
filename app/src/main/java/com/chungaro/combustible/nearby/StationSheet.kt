package com.chungaro.combustible.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Ficha de una bomba: todos sus combustibles, cómo llegar y tendencia del precio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSheet(vm: NearbyViewModel, stationId: Long, defaultFuelId: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entries = FUEL_TYPES.mapNotNull { f -> vm.stationsOf(f.id)?.firstOrNull { it.id == stationId }?.let { f to it } }
    val first = entries.firstOrNull()?.second
    DetailSheet(
        title = first?.let { stationTitle(it) } ?: "Bomba",
        subtitle = "Ficha da bomba",
        onDismiss = onDismiss,
    ) {
        if (entries.isEmpty()) {
            Text("Sem dados para esta bomba.", Modifier.padding(24.dp))
            return@DetailSheet
        }
        val base = entries.first().second
        var selFuel by rememberSaveable(stationId) {
            mutableIntStateOf(entries.firstOrNull { it.first.id == defaultFuelId }?.first?.id ?: entries.first().first.id)
        }
        val km = vm.location?.let { (lat, lon) -> distanceKm(lat, lon, base.lat, base.lon) }
        val points by remember(selFuel, stationId) { vm.history(selFuel, stationId) }
            .collectAsStateWithLifecycle(emptyList())
        val selected = FUEL_TYPES.first { it.id == selFuel }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Cabecera
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.inverseSurface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandBadge(base.brand, size = 52.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            stationTitle(base),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                        Text(
                            listOfNotNull(km?.let { "➤ " + formatKm(it) }, base.locality.ifBlank { null }).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                        )
                        if (base.brand.isNotBlank() && !base.name.contains(base.brand, ignoreCase = true)) {
                            Text(base.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f))
                        }
                    }
                    Button(
                        onClick = { openMaps(context, base) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) { Text("Ir") }
                }
            }
            if (base.address.isNotBlank()) {
                Text(base.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Combustibles de esta bomba
            Card(Modifier.fillMaxWidth()) {
                Column {
                    entries.forEachIndexed { i, (fuel, st) ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        val isSel = fuel.id == selFuel
                        val avg = (vm.rows[fuel.id] as? FuelRowState.Ready)?.result?.averageInRadius
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (isSel) fuel.uiColor.copy(alpha = 0.10f) else Color.Transparent)
                                .clickable { selFuel = fuel.id }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(fuel.uiColor))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(fuel.short, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (st.updated.isNotBlank()) {
                                    Text("Atualizado: ${st.updated}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatEuro(st.price), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                avg?.let {
                                    val d = st.price - it
                                    Text(
                                        if (d <= 0) "${formatEuro(-d)} abaixo da média" else "${formatEuro(d)} acima da média",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (d <= 0) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TrendCard("Tendência do preço · ${selected.short}", points, selected.uiColor)

            Text(
                "Preços oficiais da DGEG. A app guarda o histórico desde que a começaste a usar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
