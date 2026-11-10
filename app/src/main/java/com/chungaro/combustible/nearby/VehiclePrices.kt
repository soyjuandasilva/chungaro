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
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.emoji
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
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
import androidx.compose.ui.res.stringResource
import com.chungaro.combustible.R
import java.text.DateFormat
import java.util.Date


/**
 * Precios para tu vehículo (pestaña «Meu veículo»): consejos, la mejor opción
 * cerca de ti, la bomba más próxima y el resto de combustibles.
 */
@Composable
fun VehiclePricesSection(
    vm: NearbyViewModel,
    myFuelId: Int,
    tankLiters: Double,
    consumptionL100: Double,
    extraTips: List<Tip> = emptyList(),
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission && (!vm.started || vm.location == null)) vm.refresh()
    }
    var detailFuel by rememberSaveable { mutableStateOf<Int?>(null) }
    var stationSheet by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                    TextButton(onClick = { vm.refresh(force = true) }) { Text("Tentar outra vez") }
                }
            }
        }

        // Consejos del momento, rotando
        TipsTicker(
            vm = vm,
            myFuelId = myFuelId,
            tankLiters = tankLiters,
            consumptionL100 = consumptionL100,
            extraTips = extraTips,
            onOpenFuel = { detailFuel = it },
            onOpenStation = { stationSheet = it },
        )

        RadiusSelector(vm)

        // Lo mejor para tu vehículo
        SectionHeader("Melhores opções para o teu veículo", "A mais barata e a mais próxima para o teu combustível.")
        MyFuelCard(vm, myFuelId, tankLiters, consumptionL100, onClick = { detailFuel = myFuelId })
        NearestStationCard(vm, myFuelId, onOpen = { stationSheet = it })

        // El resto de combustibles
        SectionHeader("Outros combustíveis", "Bomba mais próxima e mais barata para cada tipo. Preços em €/L.")
        PriceTable(vm, myFuelId, onRowClick = { detailFuel = it })

        Text(
            "Toca num combustível para veres as bombas e como lá chegar.\nFonte: DGEG — precoscombustiveis.dgeg.gov.pt",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    val fuel = FUEL_TYPES.firstOrNull { it.id == detailFuel }
    val state = detailFuel?.let { vm.rows[it] }
    if (fuel != null && state is FuelRowState.Ready) {
        FuelDetailDialog(
            vm = vm,
            fuel = fuel,
            result = state.result,
            radiusKm = vm.radiusKm,
            tankLiters = tankLiters,
            consumptionL100 = consumptionL100,
            onOpenStation = { detailFuel = null; stationSheet = it },
            onDismiss = { detailFuel = null },
        )
    }
    stationSheet?.let { id ->
        StationSheet(vm, id, myFuelId, onDismiss = { stationSheet = null })
    }
}

/** Radio de búsqueda de la bomba más barata: 5, 10 o 20 km. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadiusSelector(vm: NearbyViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Procurar a", style = MaterialTheme.typography.labelLarge)
        NearbyViewModel.RADIUS_OPTIONS.forEach { km ->
            FilterChip(
                selected = km == vm.radiusKm,
                onClick = { vm.selectRadius(km) },
                label = { Text("$km km") },
            )
        }
    }
}

@Composable
private fun NearestStationCard(vm: NearbyViewModel, myFuelId: Int, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val nearest = (vm.rows[myFuelId] as? FuelRowState.Ready)?.result?.nearest ?: return
    Card(Modifier.fillMaxWidth().clickable { onOpen(nearest.station.id) }) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(nearest.station.brand, size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Bomba mais próxima", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stationTitle(nearest.station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(formatKm(nearest.km), nearest.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatEuro(nearest.station.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { openMaps(context, nearest.station) }) { Text("Ir") }
            }
        }
    }
}

@Composable
private fun AdviceLine(good: Boolean, title: String, detail: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (good) Color(0xFF16A34A).copy(alpha = 0.10f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (good) "✅" else "✋", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (good) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                )
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Variación semanal de la media de la zona para un combustible (↘ −1,2 %). */
@Composable
private fun WeeklyChangeBadge(vm: NearbyViewModel, fuelId: Int) {
    val zone by remember(fuelId) { vm.history(fuelId, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    val pct = remember(zone) { computeWeekly(zone, emptyList()).changePct } ?: return
    val up = pct > 0.05
    val down = pct < -0.05
    Text(
        (if (up) "↗ +" else if (down) "↘ " else "→ ") + String.format(java.util.Locale.getDefault(), "%.1f%%", pct) + " /sem.",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = when {
            up -> Color(0xFFDC2626)
            down -> Color(0xFF16A34A)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun MyFuelCard(
    vm: NearbyViewModel,
    myFuelId: Int,
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FuelPill(fuel)
                Text(
                    "  Melhor preço a ≤ ${vm.radiusKm} km",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(cheapest.station.brand, size = 44.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stationTitle(cheapest.station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(formatKm(cheapest.km), cheapest.station.locality).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        tablePrice(cheapest.station.price),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text("€/L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when {
                nearest == null -> Unit
                nearest?.station?.id == cheapest.station.id -> AdviceLine(
                    good = true,
                    title = "É também a bomba mais próxima",
                    detail = "Não precisas de ir mais longe para pagar menos.",
                )
                advice != null -> AdviceLine(
                    good = advice.worthIt,
                    title = if (advice.worthIt) "Vale a pena ir à mais barata" else "Não compensa ir à mais barata",
                    detail = "Face à mais próxima (${nearest?.km?.let { formatKm(it) } ?: ""}): poupas ${formatMoney(advice.savings)} em ${tankLiters.toInt()} L − " +
                        "${formatMoney(advice.tripCost)} de combustível do desvio = " +
                        (if (advice.net >= 0) "" else "−") + formatMoney(kotlin.math.abs(advice.net)) + ".",
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
                HeaderCell("Tipo", Modifier.weight(1.4f), TextAlign.Start)
                HeaderCell("Mais\npróxima", Modifier.weight(1f), TextAlign.End)
                HeaderCell("Mais barata\n≤ ${vm.radiusKm} km", Modifier.weight(1.1f), TextAlign.End)
            }
            val others = FUEL_TYPES.filter { it.id != myFuelId }
            others.forEachIndexed { i, fuel ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                FuelRow(vm, fuel, vm.rows[fuel.id], highlight = false, onClick = { onRowClick(fuel.id) })
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
private fun FuelRow(vm: NearbyViewModel, fuel: FuelType, state: FuelRowState?, highlight: Boolean, onClick: () -> Unit) {
    val ready = state as? FuelRowState.Ready
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) fuel.uiColor.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(enabled = ready != null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FuelPill(fuel, compact = true)
            WeeklyChangeBadge(vm, fuel.id)
        }
        when (state) {
            null, FuelRowState.Loading -> Box(Modifier.weight(2.1f), contentAlignment = Alignment.CenterEnd) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            is FuelRowState.Error -> Text(
                state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(2.1f),
            )
            is FuelRowState.Ready -> {
                val r = state.result
                PriceCell(r.nearest, Modifier.weight(1f), highlight = false)
                PriceCell(r.cheapest, Modifier.weight(1.1f), highlight = true)
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
                    tablePrice(s.station.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        } else {
            Text(tablePrice(s.station.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(2.dp))
        Text(formatKm(s.km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun tablePrice(v: Double): String = String.format(java.util.Locale.getDefault(), "%.3f", v)
