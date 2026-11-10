package com.chungaro.combustible.nearby

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chungaro.combustible.data.PricePoint
import com.chungaro.combustible.news.NewsApi
import com.chungaro.combustible.news.NewsItem
import com.chungaro.combustible.ui.APP_LOCALE
import com.chungaro.combustible.ui.AtestaBorder
import com.chungaro.combustible.ui.AtestaCard
import com.chungaro.combustible.ui.AtestaGreenLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val Star = Color(0xFFFBBF24)

/** Fondo de una casilla de combustible: su color oscurecido, para que el texto blanco se lea. */
private fun tileColor(c: Color, k: Float = 0.55f): Color = Color(c.red * k, c.green * k, c.blue * k)

// ---------------------------------------------------------------- Preços

@Composable
fun PricesTab(
    vm: NearbyViewModel,
    fuelId: Int,
    onFuel: (Int) -> Unit,
    favorites: List<Long>,
    onToggleFavorite: (Long) -> Unit,
    onOpenStation: (Long) -> Unit,
    onCalculator: () -> Unit,
    hasPermission: Boolean,
    onRequestLocation: () -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val version = vm.pricesVersion
    val fuel = FUEL_TYPES.first { it.id == fuelId }
    val ready = (vm.rows[fuelId] as? FuelRowState.Ready)?.result
    val place = ready?.nearest?.station?.locality?.ifBlank { null } ?: "A tua zona"

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Localización y radio
        Box {
            var open by remember { mutableStateOf(false) }
            Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = AtestaCard, border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = AtestaGreenLight, modifier = Modifier.size(20.dp))
                    Text("  $place (raio: ${vm.radiusKm} km)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Mudar o raio")
                }
            }
            androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                NearbyViewModel.RADIUS_OPTIONS.forEach { km ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Raio de $km km", fontWeight = if (km == vm.radiusKm) FontWeight.ExtraBold else FontWeight.Normal) },
                        onClick = { open = false; vm.selectRadius(km) },
                    )
                }
            }
        }

        if (!hasPermission) {
            DarkCard { Column(Modifier.padding(16.dp)) { LocationPrompt(onRequestLocation) } }
        }

        // Casillas de combustible: media en el radio (y en Portugal)
        FUEL_TYPES.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f ->
                    val r = (vm.rows[f.id] as? FuelRowState.Ready)?.result
                    val national = remember(version, f.id) { vm.nationalPrice(f.id) }
                    val sel = f.id == fuelId
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .shadow(if (sel) 12.dp else 6.dp, shape, spotColor = f.uiColor, ambientColor = Color.Black)
                            .background(Brush.verticalGradient(listOf(tileColor(f.uiColor, 0.85f), tileColor(f.uiColor, 0.42f))), shape)
                            .border(BorderStroke(if (sel) 2.dp else 1.dp, if (sel) SolidColor(Color.White) else Brush.verticalGradient(listOf(f.uiColor, Color.Transparent))), shape)
                            .clip(shape)
                            .clickable { onFuel(f.id) },
                    ) {
                        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(f.short, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                            Text(
                                (r?.averageInRadius ?: national?.average)?.let { euro3(it) } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                            national?.let { Text("PT ${euro3(it.average)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f)) }
                        }
                    }
                }
            }
        }
        Text(
            "Preço médio na tua zona; em baixo, a média de todas as bombas de Portugal. Em cada bomba o preço é diferente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Postos mais baratos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        val list = ready?.cheapestList.orEmpty()
        val all = remember(version, fuelId, vm.radiusKm, vm.location) {
            val loc = vm.location ?: return@remember emptyList<StationAtDistance>()
            vm.stationsOf(fuelId).orEmpty()
                .map { StationAtDistance(it, distanceKm(loc.first, loc.second, it.lat, it.lon)) }
                .filter { it.km <= vm.radiusKm }
                .sortedWith(compareBy({ it.station.price }, { it.km }))
        }
        val shown = if (showAll) all else (all.ifEmpty { list }).take(3)
        if (shown.isEmpty()) {
            Text("Sem dados de preços ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            DarkCard {
                shown.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = AtestaBorder)
                    StationRow(s.station, s.km, onClick = { onOpenStation(s.station.id) }) {
                        PriceChip(s.station.price, caption = fuel.short)
                        val fav = s.station.id in favorites
                        IconButton(onClick = { onToggleFavorite(s.station.id) }) {
                            Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = if (fav) "Tirar dos favoritos" else "Guardar nos favoritos", tint = if (fav) Star else Color.White)
                        }
                    }
                }
            }
        }
        if (all.size > 3) {
            OutlinedButton(
                onClick = { showAll = !showAll },
                border = BorderStroke(1.dp, AtestaGreenLight),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (showAll) "Ver menos" else "Ver todos os postos (${all.size})", color = Color.White, fontWeight = FontWeight.Bold) }
        }

        // Calculadora
        Surface(onClick = onCalculator, shape = RoundedCornerShape(18.dp), color = Color(0xFF14532D), border = BorderStroke(1.dp, AtestaGreenLight.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Calculate, contentDescription = null, tint = AtestaGreenLight, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Calculadora de poupança", fontWeight = FontWeight.ExtraBold)
                    Text("Quanto pagas por um depósito em cada posto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------- Notícias

private val CATEGORY_STYLE: Map<String, Pair<ImageVector, Color>> = mapOf(
    "Preços" to (Icons.Outlined.LocalGasStation to Color(0xFF16A34A)),
    "Mercado" to (Icons.Outlined.ShowChart to Color(0xFF2563EB)),
    "Dicas" to (Icons.Outlined.Lightbulb to Color(0xFFD97706)),
    "Mobilidade" to (Icons.Outlined.ElectricCar to Color(0xFF7C3AED)),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsTab(vm: NearbyViewModel, fuelId: Int) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.loadNews()
        vm.loadNewsCategories()
    }
    var cat by rememberSaveable { mutableStateOf("Todas") }
    val byCat: Map<String, List<NewsItem>> = mapOf("Preços" to vm.news.take(8)) + vm.newsByCategory
    val items: List<Pair<String, NewsItem>> = remember(cat, vm.news, vm.newsByCategory) {
        val pairs = if (cat == "Todas") byCat.flatMap { (c, l) -> l.map { c to it } } else byCat[cat].orEmpty().map { cat to it }
        pairs.distinctBy { it.second.link }.sortedByDescending { it.second.publishedAt ?: 0L }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MondayCard(vm, FUEL_TYPES.firstOrNull { it.id == fuelId } ?: FUEL_TYPES.first())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("Todas") + NewsApi.CATEGORIES.map { it.first }).forEach { c ->
                FilterPill(c, selected = c == cat, onClick = { cat = c })
            }
        }
        when {
            items.isEmpty() && vm.newsLoading -> Text("A carregar notícias…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            items.isEmpty() -> Text(
                if (vm.newsError) "Não conseguimos carregar as notícias. Verifica a ligação." else "Sem notícias de momento.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> items.forEach { (c, n) ->
                val (icon, color) = CATEGORY_STYLE[c] ?: (Icons.Outlined.LocalGasStation to AtestaGreenLight)
                Box(Modifier.fillMaxWidth().glassCard().clickable { openLink(context, n.link) }) {
                    Row(Modifier.padding(10.dp)) {
                        // Miniatura: el feed no trae imágenes, se usa el icono de la categoría.
                        Box(
                            Modifier.size(width = 96.dp, height = 96.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .shadow(6.dp, RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(color, tileColor(color, 0.35f))), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent))), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp)) }
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (n.snippet.isNotBlank()) {
                                Text(n.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(n.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF16A34A)) {
                                    Text(c, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                                }
                                n.publishedAt?.let { Text("  " + agoText(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
        Text("Notícias via Google Notícias. Os textos pertencem aos respetivos meios.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------- Favoritos

@Composable
fun FavoritesTab(
    vm: NearbyViewModel,
    fuelId: Int,
    favorites: List<Long>,
    onToggleFavorite: (Long) -> Unit,
    onOpenStation: (Long) -> Unit,
    alertsEnabled: Boolean,
    onToggleAlerts: (Boolean) -> Unit,
    onAlerts: () -> Unit,
    onSearchByDistrict: () -> Unit,
) {
    val version = vm.pricesVersion
    val fuel = FUEL_TYPES.first { it.id == fuelId }
    val byId = remember(version) { vm.allStations().associateBy { it.id } }
    val prices = remember(version, fuelId) { vm.stationsOf(fuelId)?.associateBy { it.id }.orEmpty() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (favorites.isEmpty()) {
            DarkCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não tens favoritos", fontWeight = FontWeight.ExtraBold)
                    Text("Toca na ☆ de um posto (ou no ♥ da ficha) para o guardares aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            DarkCard {
                favorites.forEachIndexed { i, id ->
                    if (i > 0) HorizontalDivider(color = AtestaBorder)
                    val s = byId[id]
                    if (s == null) {
                        Text("Posto $id (sem dados)", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        StationRow(s, null, onClick = { onOpenStation(id) }, subtitle = listOf(s.address, s.locality).filter { it.isNotBlank() }.joinToString(", ")) {
                            prices[id]?.let { PriceChip(it.price, caption = fuel.short) }
                            IconButton(onClick = { onToggleFavorite(id) }) { Icon(Icons.Filled.Star, contentDescription = "Tirar dos favoritos", tint = Star) }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onSearchByDistrict, border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.fillMaxWidth()) {
            Text("Procurar postos por distrito", color = Color.White)
        }

        Surface(onClick = onAlerts, shape = RoundedCornerShape(18.dp), color = Color(0xFF0F3A26), border = BorderStroke(1.dp, AtestaGreenLight.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = AtestaGreenLight, modifier = Modifier.size(34.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Alertas de preço", fontWeight = FontWeight.ExtraBold)
                    Text("Recebe um aviso quando o preço descer nas tuas estações favoritas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onToggleAlerts,
                    colors = SwitchDefaults.colors(checkedTrackColor = AtestaGreenLight, checkedThumbColor = Color.White),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------- Histórico

private val PERIODS = listOf(7 to "7 dias", 30 to "30 dias", 90 to "3 meses", 365 to "1 ano")
private val axisDate = DateTimeFormatter.ofPattern("d MMM", APP_LOCALE)

@Composable
fun HistoryTab(vm: NearbyViewModel, fuelId: Int, onFuel: (Int) -> Unit) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var days by rememberSaveable { mutableIntStateOf(30) }
    val fuel = FUEL_TYPES.first { it.id == fuelId }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Evolução | Comparar
        Surface(shape = RoundedCornerShape(14.dp), color = AtestaCard, border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                listOf("Evolução", "Comparar").forEachIndexed { i, label ->
                    Surface(
                        onClick = { mode = i },
                        shape = RoundedCornerShape(10.dp),
                        color = if (mode == i) Color(0xFF16A34A) else Color.Transparent,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        }
        // Periodo
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PERIODS.forEach { (d, label) ->
                Surface(
                    onClick = { days = d },
                    shape = RoundedCornerShape(10.dp),
                    color = if (d == days) AtestaGreenLight else AtestaCard,
                    border = BorderStroke(1.dp, if (d == days) AtestaGreenLight else AtestaBorder),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (d == days) Color(0xFF052E16) else Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }

        if (mode == 0) {
            Box {
                var open by remember { mutableStateOf(false) }
                Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = AtestaCard, border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(fuel.uiColor, 10.dp)
                        Text("  " + fuel.short, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Mudar combustível")
                    }
                }
                androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    FUEL_TYPES.forEach { f ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(f.short) }, onClick = { open = false; onFuel(f.id) })
                    }
                }
            }
            val points by remember(fuelId, days) { vm.historyRange(fuelId, HistoryRecorder.ZONE_AVG, days) }.collectAsStateWithLifecycle(emptyList())
            DarkCard {
                Box(Modifier.padding(12.dp)) {
                    if (points.size < 2) {
                        Text(
                            "A recolher histórico. A app guarda os preços da tua zona várias vezes por dia; o gráfico aparece a partir do segundo dia.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        PriceLineChart(points, fuel.uiColor)
                    }
                }
            }
            if (points.size >= 2) {
                val first = points.first().price
                val last = points.last().price
                val diff = last - first
                val pct = diff / first * 100
                val up = diff > 0
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = if (up) Color(0xFF3B1212) else Color(0xFF0F3A26), border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (if (up) "↑ +" else "↓ −") + String.format(Locale.getDefault(), "%.3f€", abs(diff)),
                                color = if (up) Color(0xFFF87171) else AtestaGreenLight,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text((if (up) "+" else "−") + String.format(Locale.getDefault(), "%.1f%%", abs(pct)), fontWeight = FontWeight.Bold)
                            Text("(últimos ${PERIODS.first { it.first == days }.second})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = AtestaCard, border = BorderStroke(1.dp, AtestaBorder), modifier = Modifier.weight(1f)) {
                        Row(Modifier.padding(12.dp)) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mínimo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(euro3(points.minOf { it.price }), fontWeight = FontWeight.ExtraBold)
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Máximo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(euro3(points.maxOf { it.price }), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
            Text("Preço médio das bombas a menos de ${HistoryRecorder.ZONE_KM.toInt()} km de ti.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Comparar: todos los combustibles, ahora frente al inicio del periodo
            DarkCard {
                FUEL_TYPES.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = AtestaBorder)
                    val pts by remember(f.id, days) { vm.historyRange(f.id, HistoryRecorder.ZONE_AVG, days) }.collectAsStateWithLifecycle(emptyList())
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(f.uiColor, 10.dp)
                        Text("  " + f.short, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (pts.size >= 2) {
                            val d = pts.last().price - pts.first().price
                            Text(
                                (if (d > 0) "↑ +" else if (d < 0) "↓ −" else "→ ") + String.format(Locale.getDefault(), "%.3f", abs(d)),
                                color = if (d > 0) Color(0xFFF87171) else if (d < 0) AtestaGreenLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        pts.lastOrNull()?.let { PriceChip(it.price, f.uiColor) } ?: Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Variação do preço médio da tua zona nos últimos ${PERIODS.first { it.first == days }.second}.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Gráfico de líneas con ejes, relleno degradado y el último valor destacado. */
@Composable
fun PriceLineChart(points: List<PricePoint>, color: Color) {
    val sorted = points.sortedBy { it.day }
    val min = sorted.minOf { it.price }
    val max = sorted.maxOf { it.price }
    val pad = ((max - min) * 0.15).coerceAtLeast(0.01)
    val lo = min - pad
    val hi = max + pad
    val firstDay = sorted.first().day
    val lastDay = sorted.last().day
    val span = (lastDay - firstDay).coerceAtLeast(1)
    val last = sorted.last()
    val lineColor = if (color.luminance() < 0.2f) AtestaGreenLight else color

    Column {
        Row {
            // Eje Y
            Column(Modifier.height(180.dp).width(56.dp), verticalArrangement = Arrangement.SpaceBetween) {
                (0..4).forEach { i ->
                    val v = hi - (hi - lo) * i / 4
                    Text(String.format(Locale.getDefault(), "%.3f", v), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.weight(1f).height(180.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    fun x(day: Long) = (day - firstDay).toFloat() / span * size.width
                    fun y(p: Double) = ((hi - p) / (hi - lo)).toFloat() * size.height
                    // Rejilla
                    (0..4).forEach { i ->
                        val gy = size.height * i / 4
                        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, gy), Offset(size.width, gy), 1f)
                    }
                    val line = Path()
                    sorted.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(p.day), y(p.price)) else line.lineTo(x(p.day), y(p.price)) }
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(x(lastDay), size.height)
                        lineTo(x(firstDay), size.height)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)))
                    drawPath(line, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    drawCircle(Color.White, 9f, Offset(x(last.day), y(last.price)))
                    drawCircle(lineColor, 6f, Offset(x(last.day), y(last.price)))
                }
                // Etiqueta del último valor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F3A26),
                    border = BorderStroke(1.dp, lineColor),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(euro3(last.price), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                        Text(LocalDate.ofEpochDay(last.day).format(axisDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Eje X
        Row(Modifier.fillMaxWidth().padding(start = 56.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..3).forEach { i ->
                val d = firstDay + span * i / 3
                Text(LocalDate.ofEpochDay(d).format(axisDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
