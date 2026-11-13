package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.AbasteceHoje
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
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
import com.chungaro.combustible.ui.BrandBorder
import com.chungaro.combustible.ui.BrandCard
import com.chungaro.combustible.ui.BrandGreenLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs


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
            Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = BrandCard, border = BorderStroke(1.dp, BrandBorder), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = BrandGreenLight, modifier = Modifier.size(20.dp))
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
                    val shape = RoundedCornerShape(12.dp)
                    val cs = MaterialTheme.colorScheme
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (sel) cs.primaryContainer else AbasteceHoje.colors.card, shape)
                            .border(if (sel) 2.dp else 1.dp, if (sel) cs.primary else AbasteceHoje.colors.cardBorder, shape)
                            .selectable(selected = sel, role = Role.RadioButton, onClick = { onFuel(f.id) }),
                    ) {
                        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Dot(f.uiColor, 8.dp)
                                Spacer(Modifier.width(5.dp))
                                Text(f.short, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                (r?.averageInRadius ?: national?.average)?.let { euro3(it) } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                            national?.let { Text("PT ${euro3(it.average)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
        Text(
            "Em cima, o preço médio na tua zona; em baixo, a média de todos os postos de Portugal. Em cada posto o preço é diferente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        BlockTitle("Postos mais baratos", Modifier.padding(top = 8.dp))
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
            val state = vm.rows[fuelId]
            if (state is FuelRowState.Error && vm.stationsOf(fuelId).isNullOrEmpty()) {
                DarkCard { ErrorRetry("Sem ligação: não conseguimos descarregar os preços.", onRetry = { vm.refresh(force = true) }) }
            } else {
                Text(
                    if (vm.location == null) "Ativa a localização para veres os postos mais baratos perto de ti." else "A carregar os preços…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DarkCard {
                shown.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = BrandBorder)
                    StationRow(s.station, s.km, onClick = { onOpenStation(s.station.id) }) {
                        PriceChip(s.station.price, caption = fuel.short)
                        val fav = s.station.id in favorites
                        IconButton(onClick = { onToggleFavorite(s.station.id) }) {
                            Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = if (fav) "Tirar dos favoritos" else "Guardar nos favoritos", tint = if (fav) AbasteceHoje.colors.star else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (all.size > 3) {
            OutlinedButton(
                onClick = { showAll = !showAll },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (showAll) "Ver menos" else "Ver todos os postos (${all.size})", fontWeight = FontWeight.Bold) }
        }

        // Calculadora
        Surface(
            onClick = onCalculator,
            shape = CardShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Calculadora de poupança", fontWeight = FontWeight.Bold)
                    Text("Quanto pagas por um depósito em cada posto", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------- Notícias

/** Icono y color de cada categoría de noticias, según el tema. */
@Composable
private fun categoryStyle(category: String): Pair<ImageVector, Color> = when (category) {
    "Mercado" -> Icons.Outlined.ShowChart to AbasteceHoje.colors.info
    "Dicas" -> Icons.Outlined.Lightbulb to AbasteceHoje.colors.warning
    "Mobilidade" -> Icons.Outlined.ElectricCar to AbasteceHoje.colors.purple
    else -> Icons.Outlined.LocalGasStation to AbasteceHoje.colors.accent
}

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
            items.isEmpty() && vm.newsError -> DarkCard {
                ErrorRetry("Não conseguimos carregar as notícias. Verifica a ligação.", onRetry = { vm.loadNews(force = true); vm.loadNewsCategories() })
            }
            items.isEmpty() -> Text("Sem notícias de momento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> items.forEach { (c, n) ->
                val (icon, color) = categoryStyle(c)
                Box(Modifier.fillMaxWidth().cardSurface().clickable(onClickLabel = "Abrir a notícia") { openLink(context, n.link) }) {
                    Row(Modifier.padding(12.dp)) {
                        // El feed no trae imágenes: icono de la categoría sobre su color suave.
                        Box(
                            Modifier.size(48.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp)) }
                        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (n.snippet.isNotBlank()) {
                                Text(n.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text(n.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(c, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
                                n.publishedAt?.let { Text(" · " + agoText(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
        Text("Notícias via Google Notícias. Os textos pertencem aos respetivos meios.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Ainda não tens favoritos", fontWeight = FontWeight.Bold)
                    Text("Toca na estrela de um posto para o guardares aqui e veres o preço de um relance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            DarkCard {
                favorites.forEachIndexed { i, id ->
                    if (i > 0) HorizontalDivider(color = BrandBorder)
                    val s = byId[id]
                    if (s == null) {
                        Text("Posto $id (sem dados)", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        StationRow(s, null, onClick = { onOpenStation(id) }, subtitle = listOf(s.address, s.locality).filter { it.isNotBlank() }.joinToString(", ")) {
                            prices[id]?.let { PriceChip(it.price, caption = fuel.short) }
                            IconButton(onClick = { onToggleFavorite(id) }) { Icon(Icons.Filled.Star, contentDescription = "Tirar dos favoritos", tint = AbasteceHoje.colors.star) }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onSearchByDistrict, modifier = Modifier.fillMaxWidth()) {
            Text("Procurar postos por distrito")
        }

        Surface(onClick = onAlerts, shape = CardShape, color = AbasteceHoje.colors.card, border = BorderStroke(1.dp, AbasteceHoje.colors.cardBorder), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = AbasteceHoje.colors.accent, modifier = Modifier.size(28.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Alertas de preço", fontWeight = FontWeight.Bold)
                    Text("Recebe um aviso quando o preço descer nos teus postos favoritos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onToggleAlerts,
                    modifier = Modifier.semantics { contentDescription = "Alertas de preço" },
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
        val cs = MaterialTheme.colorScheme
        Surface(shape = RoundedCornerShape(14.dp), color = AbasteceHoje.colors.card, border = BorderStroke(1.dp, AbasteceHoje.colors.cardBorder), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp).selectableGroup()) {
                listOf("Evolução", "Comparar").forEachIndexed { i, label ->
                    val sel = mode == i
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) cs.primary else Color.Transparent)
                            .selectable(selected = sel, role = Role.Tab, onClick = { mode = i }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = if (sel) cs.onPrimary else cs.onSurface)
                    }
                }
            }
        }
        // Periodo
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PERIODS.forEach { (d, label) ->
                val sel = d == days
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) cs.primaryContainer else AbasteceHoje.colors.card)
                        .border(1.dp, if (sel) cs.primary else cs.outline, RoundedCornerShape(10.dp))
                        .selectable(selected = sel, role = Role.RadioButton, onClick = { days = d }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (sel) cs.onPrimaryContainer else cs.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        if (mode == 0) {
            Box {
                var open by remember { mutableStateOf(false) }
                Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = BrandCard, border = BorderStroke(1.dp, BrandBorder), modifier = Modifier.fillMaxWidth()) {
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
                    Surface(shape = CardShape, color = if (up) AbasteceHoje.colors.upContainer else AbasteceHoje.colors.downContainer, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                (if (up) "↑ +" else "↓ −") + String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f€", abs(diff)),
                                color = if (up) AbasteceHoje.colors.up else AbasteceHoje.colors.down,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text((if (up) "+" else "−") + String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.1f%%", abs(pct)), fontWeight = FontWeight.Bold)
                            Text("últimos ${PERIODS.first { it.first == days }.second}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(shape = CardShape, color = AbasteceHoje.colors.card, border = BorderStroke(1.dp, AbasteceHoje.colors.cardBorder), modifier = Modifier.weight(1f)) {
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
            Text("Preço médio dos postos a menos de ${HistoryRecorder.ZONE_KM.toInt()} km de ti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Comparar: todos los combustibles, ahora frente al inicio del periodo
            DarkCard {
                FUEL_TYPES.forEachIndexed { i, f ->
                    if (i > 0) HorizontalDivider(color = BrandBorder)
                    val pts by remember(f.id, days) { vm.historyRange(f.id, HistoryRecorder.ZONE_AVG, days) }.collectAsStateWithLifecycle(emptyList())
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(f.uiColor, 10.dp)
                        Text("  " + f.short, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (pts.size >= 2) {
                            val d = pts.last().price - pts.first().price
                            Text(
                                (if (d > 0) "↑ +" else if (d < 0) "↓ −" else "→ ") + String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f", abs(d)),
                                color = if (d > 0) AbasteceHoje.colors.up else if (d < 0) AbasteceHoje.colors.down else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        pts.lastOrNull()?.let { PriceChip(it.price, f.uiColor) } ?: Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Variação do preço médio da tua zona nos últimos ${PERIODS.first { it.first == days }.second}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val lineColor = color
    val grid = MaterialTheme.colorScheme.outlineVariant
    val ring = AbasteceHoje.colors.card

    Column {
        Row {
            // Eje Y
            Column(Modifier.height(180.dp).width(56.dp), verticalArrangement = Arrangement.SpaceBetween) {
                (0..4).forEach { i ->
                    val v = hi - (hi - lo) * i / 4
                    Text(String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f", v), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.weight(1f).height(180.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    fun x(day: Long) = (day - firstDay).toFloat() / span * size.width
                    fun y(p: Double) = ((hi - p) / (hi - lo)).toFloat() * size.height
                    // Rejilla
                    (0..4).forEach { i ->
                        val gy = size.height * i / 4
                        drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1f)
                    }
                    val line = Path()
                    sorted.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(p.day), y(p.price)) else line.lineTo(x(p.day), y(p.price)) }
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(x(lastDay), size.height)
                        lineTo(x(firstDay), size.height)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.20f), Color.Transparent)))
                    drawPath(line, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    drawCircle(ring, 9f, Offset(x(last.day), y(last.price)))
                    drawCircle(lineColor, 6f, Offset(x(last.day), y(last.price)))
                }
                // Etiqueta del último valor
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
