package com.chungaro.combustible.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.chungaro.combustible.ui.appBackground
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chungaro.combustible.prices.DgegApi
import com.chungaro.combustible.ui.AtestaBorder
import com.chungaro.combustible.ui.AtestaCard
import com.chungaro.combustible.ui.AtestaGreenLight
import com.chungaro.combustible.ui.StatusBarIcons
import kotlinx.coroutines.delay

const val HOME_MAP = 0
const val HOME_PRICES = 1
const val HOME_NEWS = 2
const val HOME_FAVORITES = 3
const val HOME_HISTORY = 4

const val SORT_CHEAPEST = 0
const val SORT_NEAREST = 1
const val SORT_FAVORITES = 2
val SORT_LABELS = listOf("Mais barato", "Mais próximo", "Favoritos")

/**
 * Pantalla principal: cabecera con el logotipo, avisos y definiciones,
 * buscador, pestañas (Mapa, Preços, Notícias, Favoritos, Histórico) y su contenido.
 */
@Composable
fun MainHome(
    vm: NearbyViewModel,
    myFuelId: Int,
    tankLiters: Double,
    favorites: List<Long>,
    onToggleFavorite: (Long) -> Unit,
    alertsEnabled: Boolean,
    onToggleAlerts: (Boolean) -> Unit,
    onAlerts: () -> Unit,
    onSettings: () -> Unit,
    onCalculator: () -> Unit,
    onSearchByDistrict: () -> Unit,
) {
    StatusBarIcons(darkBackground = true)
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
    }
    val requestLocation = {
        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { if (!vm.started) vm.refresh() }
    // Recarga sola: al volver a la app y cada 10 minutos.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vm.started) vm.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10 * 60_000L)
            vm.refresh()
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(HOME_MAP) }
    var fuelId by rememberSaveable(myFuelId) { mutableIntStateOf(myFuelId) }
    var query by rememberSaveable { mutableStateOf("") }
    var stationId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openStation: (Long) -> Unit = { stationId = it }

    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.appBackground()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Cabecera
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Logotipo: círculo verde con brillo
                Box(
                    Modifier
                        .size(46.dp)
                        .shadow(12.dp, CircleShape, spotColor = AtestaGreenLight, ambientColor = AtestaGreenLight)
                        .background(GreenBrush, CircleShape)
                        .border(BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent))), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Text("Atesta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                IconButton(onClick = onAlerts) { Icon(Icons.Outlined.NotificationsNone, contentDescription = "Alertas de preço") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Definições") }
            }
            SearchField(query, onChange = { query = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            HomeTabs(tab, onSelect = { tab = it; query = "" })
            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (query.isNotBlank()) {
                    SearchResults(vm, query, fuelId, onOpenStation = openStation)
                } else {
                    when (tab) {
                        HOME_MAP -> MapTab(vm, fuelId, { fuelId = it }, favorites, hasPermission, requestLocation, openStation, onOpenNews = { tab = HOME_NEWS })
                        HOME_PRICES -> PricesTab(vm, fuelId, { fuelId = it }, favorites, onToggleFavorite, openStation, onCalculator, hasPermission, requestLocation)
                        HOME_NEWS -> NewsTab(vm, fuelId)
                        HOME_FAVORITES -> FavoritesTab(vm, fuelId, favorites, onToggleFavorite, openStation, alertsEnabled, onToggleAlerts, onAlerts, onSearchByDistrict)
                        else -> HistoryTab(vm, fuelId, { fuelId = it })
                    }
                }
            }
        }
    }

    stationId?.let { id ->
        StationSheet(
            vm, id, fuelId,
            onDismiss = { stationId = null },
            isFavorite = id in favorites,
            onToggleFavorite = { onToggleFavorite(id) },
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().glassCard(RoundedCornerShape(50), elevation = 6.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f).padding(start = 10.dp)) {
                if (value.isEmpty()) {
                    Text("Procurar posto, morada ou marca", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    cursorBrush = SolidColor(AtestaGreenLight),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Limpar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).clickableNoRipple { onChange("") },
                )
            }
        }
    }
}

@Composable
private fun HomeTabs(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Outlined.Place to "Mapa",
        Icons.Outlined.LocalGasStation to "Preços",
        Icons.Outlined.Newspaper to "Notícias",
        Icons.Outlined.StarBorder to "Favoritos",
        Icons.Outlined.History to "Histórico",
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).glassCard(RoundedCornerShape(18.dp), elevation = 8.dp).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == selected
            // Separador fino entre pestañas no seleccionadas
            if (i > 0 && !sel && i - 1 != selected) {
                Box(Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.12f)))
            }
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (sel) Modifier
                            .shadow(10.dp, shape, spotColor = AtestaGreenLight, ambientColor = AtestaGreenLight)
                            .background(GreenBrush, shape)
                            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent))), shape)
                        else Modifier,
                    )
                    .clip(shape)
                    .clickable { onSelect(i) },
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, contentDescription = null, tint = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Resultados del buscador: bombas por nombre, marca, localidad o morada. */
@Composable
private fun SearchResults(vm: NearbyViewModel, query: String, fuelId: Int, onOpenStation: (Long) -> Unit) {
    val q = DgegApi.normalize(query)
    val version = vm.pricesVersion
    val results = remember(q, version, vm.location) {
        val loc = vm.location
        vm.allStations()
            .filter { DgegApi.normalize("${it.name} ${it.brand} ${it.locality} ${it.address}").contains(q) }
            .map { s -> s to loc?.let { (lat, lon) -> distanceKm(lat, lon, s.lat, s.lon) } }
            .sortedBy { it.second ?: Double.MAX_VALUE }
            .take(30)
    }
    val prices = vm.stationsOf(fuelId)?.associateBy { it.id }.orEmpty()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
        if (results.isEmpty()) {
            Text("Nenhuma bomba encontrada para «$query».", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            return@Column
        }
        DarkCard {
            results.forEachIndexed { i, (s, km) ->
                if (i > 0) HorizontalDivider(color = AtestaBorder)
                StationRow(s, km, onClick = { onOpenStation(s.id) }, subtitle = listOfNotNull(km?.let { formatKm(it) }, s.locality.ifBlank { null }).joinToString(" · ")) {
                    prices[s.id]?.let { PriceChip(it.price) }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Pestaña Mapa: filtros, cintillo del lunes, mapa y «Mais baratos perto de ti». */
@Composable
private fun MapTab(
    vm: NearbyViewModel,
    fuelId: Int,
    onFuel: (Int) -> Unit,
    favorites: List<Long>,
    hasPermission: Boolean,
    onRequestLocation: () -> Unit,
    onOpenStation: (Long) -> Unit,
    onOpenNews: () -> Unit,
) {
    var sort by rememberSaveable { mutableIntStateOf(SORT_CHEAPEST) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var topo by rememberSaveable { mutableStateOf(false) }
    var recenter by remember { mutableIntStateOf(0) }
    val fuel = FUEL_TYPES.first { it.id == fuelId }

    val location = vm.location
    val all = remember(vm.rows[fuelId], location, vm.pricesVersion, fuelId) {
        val list = vm.stationsOf(fuelId).orEmpty()
        val (lat, lon) = location ?: return@remember emptyList<StationAtDistance>()
        list.map { StationAtDistance(it, distanceKm(lat, lon, it.lat, it.lon)) }
    }
    val onMap = all.filter { it.km <= vm.radiusKm * 1.5 }
    val inRadius = all.filter { it.km <= vm.radiusKm }
    val listed = when (sort) {
        SORT_NEAREST -> inRadius.sortedBy { it.km }
        SORT_FAVORITES -> all.filter { it.station.id in favorites }.sortedBy { it.station.price }
        else -> inRadius.sortedWith(compareBy({ it.station.price }, { it.km }))
    }.take(10)

    Column(Modifier.fillMaxSize()) {
        // Filtros
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropPill(fuel.short, FUEL_TYPES.map { it.short }, FUEL_TYPES.indexOf(fuel), selected = true) { onFuel(FUEL_TYPES[it].id) }
            DropPill("≤ ${vm.radiusKm} km", NearbyViewModel.RADIUS_OPTIONS.map { "$it km" }, NearbyViewModel.RADIUS_OPTIONS.indexOf(vm.radiusKm)) {
                vm.selectRadius(NearbyViewModel.RADIUS_OPTIONS[it])
            }
            DropPill(SORT_LABELS[sort], SORT_LABELS, sort) { sort = it }
        }
        Spacer(Modifier.height(10.dp))
        // Mapa como tarjeta redondeada con borde, igual que el diseño
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                .glassCard(RoundedCornerShape(24.dp), elevation = 12.dp),
        ) {
            OsmMap(location, onMap, recenter, topo = topo, pastel = true, onSelect = onOpenStation)

            // Botones del mapa: capas y centrar
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapButton(Icons.Outlined.Layers, if (topo) "Mapa normal" else "Mapa de relevo") { topo = !topo }
                if (hasPermission) MapButton(Icons.Outlined.MyLocation, "Centrar na minha localização") { vm.refresh(); recenter++ }
            }

            // Panel inferior, sobre el mapa
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .glassCard(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), elevation = 16.dp),
            ) {
                Column {
                    if (!hasPermission) {
                        Column(Modifier.padding(16.dp)) { LocationPrompt(onRequestLocation) }
                        return@Column
                    }
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (sort == SORT_FAVORITES) "As tuas favoritas (${listed.size})" else "Mais baratos perto de ti",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, contentDescription = if (expanded) "Recolher" else "Expandir")
                    }
                    if (expanded) {
                        Column(Modifier.heightIn(max = 230.dp).verticalScroll(rememberScrollState())) {
                            when {
                                location == null -> Text("A procurar onde estás…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                                listed.isEmpty() -> Text(
                                    if (sort == SORT_FAVORITES) "Ainda não tens favoritos. Abre um posto e toca no ♥." else "Sem postos a menos de ${vm.radiusKm} km.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                                else -> listed.forEachIndexed { i, s ->
                                    if (i > 0) HorizontalDivider(color = AtestaBorder)
                                    StationRow(s.station, s.km, onClick = { onOpenStation(s.station.id) }) {
                                        PriceChip(s.station.price, Color(0xFF22B455), big = true)
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(50.dp)
            .shadow(10.dp, CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1C3B2C), Color(0xFF07140D))), CircleShape)
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent))), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = Color.White) }
}

/** Pastilla con menú desplegable. */
@Composable
fun DropPill(text: String, options: List<String>, selectedIndex: Int, selected: Boolean = false, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterPill(text, selected = selected, onClick = { open = true }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = if (selected) AtestaGreenLight else Color.White, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(
                    text = { Text(o, fontWeight = if (i == selectedIndex) FontWeight.ExtraBold else FontWeight.Normal, color = if (i == selectedIndex) AtestaGreenLight else Color.Unspecified) },
                    onClick = { open = false; onPick(i) },
                )
            }
        }
    }
}

/** Clic sin efecto de onda (para filas de cabecera). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
