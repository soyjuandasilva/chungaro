package com.chungaro.combustible.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import java.text.DateFormat
import java.util.Date

private val HeaderBrush = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF0E7490), Color(0xFF1D4ED8)))

@Composable
fun HomeScreen(
    vm: NearbyViewModel,
    myFuelId: Int,
    vehicleName: String,
    tankLiters: Double,
    consumptionL100: Double,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission && !vm.started) vm.refresh()
    }
    var detailFuel by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(vm, hasPermission, onOpenSettings)

            if (!hasPermission) {
                PermissionCard {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                return@Column
            }

            vm.locationError?.let { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(msg, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.refresh(force = true) }) { Text("Reintentar") }
                    }
                }
            }

            MyFuelCard(vm, myFuelId, vehicleName, tankLiters, consumptionL100, onClick = { detailFuel = myFuelId })
            NearestStationCard(vm, onClick = { detailFuel = myFuelId })
            PriceTable(vm, myFuelId, onRowClick = { detailFuel = it })

            Text(
                "Toca un combustible para ver las gasolineras y cómo llegar.\nFuente: DGEG — precoscombustiveis.dgeg.gov.pt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val fuel = FUEL_TYPES.firstOrNull { it.id == detailFuel }
    val state = detailFuel?.let { vm.rows[it] }
    if (fuel != null && state is FuelRowState.Ready) {
        FuelDetailDialog(fuel, state.result, vm.radiusKm, tankLiters, consumptionL100, onDismiss = { detailFuel = null })
    }
}

@Composable
private fun Header(vm: NearbyViewModel, hasPermission: Boolean, onOpenSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HeaderBrush)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Chungaro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    val sub = when {
                        vm.locating -> "Buscando tu ubicación…"
                        vm.pricesTime != null -> "Precios cerca de ti · " +
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(vm.pricesTime!!))
                        else -> "Precios de hoy cerca de ti"
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
                if (hasPermission) {
                    IconButton(onClick = { vm.refresh(force = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White)
                }
            }
            if (hasPermission) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Radio", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    NearbyViewModel.RADIUS_OPTIONS.forEach { km ->
                        val selected = km == vm.radiusKm
                        Surface(
                            onClick = { vm.selectRadius(km) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.18f),
                            contentColor = if (selected) Color(0xFF0F766E) else Color.White,
                        ) {
                            Text(
                                "$km km",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text("¿Dónde está la gasolina más barata?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Activa tu ubicación para comparar, para cada combustible, la gasolinera más cercana con la más barata de tu zona. Precios oficiales de la DGEG.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Usar mi ubicación") }
        }
    }
}

@Composable
private fun NearestStationCard(vm: NearbyViewModel, onClick: () -> Unit) {
    val context = LocalContext.current
    val nearest = vm.rows.values
        .mapNotNull { (it as? FuelRowState.Ready)?.result?.nearest }
        .minByOrNull { it.km } ?: return
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Gasolinera más cercana", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stationTitle(nearest.station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(formatKm(nearest.km), nearest.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { openMaps(context, nearest.station) }) { Text("Ir") }
        }
    }
}

@Composable
private fun MyFuelCard(
    vm: NearbyViewModel,
    myFuelId: Int,
    vehicleName: String,
    tankLiters: Double,
    consumptionL100: Double,
    onClick: () -> Unit,
) {
    val fuel = FUEL_TYPES.firstOrNull { it.id == myFuelId } ?: return
    val r = (vm.rows[myFuelId] as? FuelRowState.Ready)?.result ?: return
    val cheapest = r.cheapest ?: return
    val nearest = r.nearest
    val advice = nearest?.let { tripAdvice(it, cheapest, tankLiters, consumptionL100) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FuelPill(fuel)
                Text(
                    "  $vehicleName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Mejor precio a ≤ ${vm.radiusKm} km", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stationTitle(cheapest.station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatKm(cheapest.km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    formatEuro(cheapest.station.price),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            when {
                nearest?.station?.id == cheapest.station.id ->
                    Text("✅ Es también la más cercana.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                advice != null -> Text(
                    if (advice.worthIt) "✅ Merece la pena: ganas ${formatMoney(advice.net)} llenando ${tankLiters.toInt()} L"
                    else "✋ No compensa ir: mejor la más cercana (${nearest?.km?.let { formatKm(it) } ?: ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (advice.worthIt) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PriceTable(vm: NearbyViewModel, myFuelId: Int, onRowClick: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell("Combustible", Modifier.weight(1.25f), TextAlign.Start)
                HeaderCell("Más cercana", Modifier.weight(1f), TextAlign.End)
                HeaderCell("Más barata\n≤ ${vm.radiusKm} km", Modifier.weight(1f), TextAlign.End)
            }
            val ordered = FUEL_TYPES.sortedByDescending { it.id == myFuelId }
            ordered.forEachIndexed { i, fuel ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                FuelRow(fuel, vm.rows[fuel.id], highlight = fuel.id == myFuelId, onClick = { onRowClick(fuel.id) })
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
private fun FuelRow(fuel: FuelType, state: FuelRowState?, highlight: Boolean, onClick: () -> Unit) {
    val ready = state as? FuelRowState.Ready
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) fuel.uiColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = ready != null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1.25f)) { FuelPill(fuel) }
        when (state) {
            null, FuelRowState.Loading -> Box(Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            is FuelRowState.Error -> Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(2f),
            )
            is FuelRowState.Ready -> {
                val r = state.result
                PriceCell(r.nearest, Modifier.weight(1f), highlight = false)
                PriceCell(r.cheapest, Modifier.weight(1f), highlight = true)
            }
        }
    }
}

@Composable
private fun PriceCell(s: StationAtDistance?, modifier: Modifier, highlight: Boolean) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        if (s == null) {
            Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        if (highlight) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    formatEuro(s.station.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        } else {
            Text(formatEuro(s.station.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(2.dp))
        Text(formatKm(s.km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
