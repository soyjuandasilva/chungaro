package com.chungaro.combustible.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.outlined.CloudOff
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import com.chungaro.combustible.prices.DgegApi
import com.chungaro.combustible.ui.Atesta
import com.chungaro.combustible.ui.StatusBarIcons
import com.chungaro.combustible.ui.appBackground
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
    StatusBarIcons()
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
    // Recarga sola al volver a la app y cada 10 minutos, solo mientras se ve
    // (en segundo plano no gasta datos ni batería).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vm.started) vm.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(10 * 60_000L)
                vm.refresh()
            }
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(HOME_MAP) }
    var fuelId by rememberSaveable(myFuelId) { mutableIntStateOf(myFuelId) }
    var query by rememberSaveable { mutableStateOf("") }
    var stationId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openStation: (Long) -> Unit = { stationId = it }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.appBackground()) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Cabecera
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
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
                        HOME_MAP -> MapTab(vm, fuelId, { fuelId = it }, favorites, hasPermission, requestLocation, openStation)
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

private const val SEARCH_HINT = "Procurar posto, morada ou marca"

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val c = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp).cardSurface(RoundedCornerShape(50)), contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(start = 14.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f).padding(start = 10.dp, top = 12.dp, bottom = 12.dp)) {
                if (value.isEmpty()) {
                    Text(SEARCH_HINT, style = MaterialTheme.typography.bodyLarge, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onSurface),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = SEARCH_HINT },
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Limpar a pesquisa", tint = c.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.size(12.dp))
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
    val c = MaterialTheme.colorScheme
    // Con letra grande las cinco pestañas no caben: pasan a desplazarse en horizontal.
    val bigText = LocalDensity.current.fontScale > 1.15f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .cardSurface(RoundedCornerShape(16.dp))
            .then(if (bigText) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == selected
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .then(if (bigText) Modifier.widthIn(min = 84.dp) else Modifier.weight(1f))
                    .clip(shape)
                    .background(if (sel) c.primary else Color.Transparent, shape)
                    .selectable(selected = sel, role = Role.Tab, onClick = { onSelect(i) }),
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tint = if (sel) c.onPrimary else c.onSurfaceVariant
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
            Text(
                "Nenhum posto encontrado para «$query». Experimenta o nome da marca ou da localidade.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }
        DarkCard {
            results.forEachIndexed { i, (s, km) ->
                if (i > 0) HorizontalDivider(color = Atesta.colors.cardBorder)
                StationRow(s, km, onClick = { onOpenStation(s.id) }, subtitle = listOfNotNull(km?.let { formatKm(it) }, s.locality.ifBlank { null }).joinToString(" · ")) {
                    prices[s.id]?.let { PriceChip(it.price) }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Pestaña Mapa: filtros, mapa y «Mais baratos perto de ti». */
@Composable
private fun MapTab(
    vm: NearbyViewModel,
    fuelId: Int,
    onFuel: (Int) -> Unit,
    favorites: List<Long>,
    hasPermission: Boolean,
    onRequestLocation: () -> Unit,
    onOpenStation: (Long) -> Unit,
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
    // Sin precios guardados: ¿se están descargando o falló la conexión?
    val noPrices = vm.stationsOf(fuelId).isNullOrEmpty()
    val rowState = vm.rows[fuelId]

    Column(Modifier.fillMaxSize()) {
        // Filtros
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DropPill(fuel.short, FUEL_TYPES.map { it.short }, FUEL_TYPES.indexOf(fuel), selected = true, label = "Combustível") { onFuel(FUEL_TYPES[it].id) }
            DropPill("≤ ${vm.radiusKm} km", NearbyViewModel.RADIUS_OPTIONS.map { "$it km" }, NearbyViewModel.RADIUS_OPTIONS.indexOf(vm.radiusKm), label = "Raio") {
                vm.selectRadius(NearbyViewModel.RADIUS_OPTIONS[it])
            }
            DropPill(SORT_LABELS[sort], SORT_LABELS, sort, label = "Ordenar por") { sort = it }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                .cardSurface(RoundedCornerShape(20.dp)),
        ) {
            OsmMap(location, onMap, recenter, topo = topo, pastel = true, onSelect = onOpenStation)

            // Botones del mapa: capas y centrar
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapButton(Icons.Outlined.Layers, if (topo) "Mostrar mapa normal" else "Mostrar mapa de relevo") { topo = !topo }
                if (hasPermission) MapButton(Icons.Outlined.MyLocation, "Centrar na minha localização") { vm.refresh(); recenter++ }
            }

            // Panel inferior, flotando sobre el mapa
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .cardSurface(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), floating = true),
            ) {
                Column {
                    if (!hasPermission) {
                        Column(Modifier.padding(16.dp)) { LocationPrompt(onRequestLocation) }
                        return@Column
                    }
                    val title = if (sort == SORT_FAVORITES) "As tuas favoritas (${listed.size})" else "Mais baratos perto de ti"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (expanded) "Recolher a lista" else "Mostrar a lista",
                            ) { expanded = !expanded }
                            .semantics { stateDescription = if (expanded) "Lista aberta" else "Lista fechada" }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, contentDescription = null)
                    }
                    if (expanded) {
                        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                            when {
                                noPrices && rowState is FuelRowState.Error -> ErrorRetry(
                                    "Sem ligação: não conseguimos descarregar os preços.",
                                    onRetry = { vm.refresh(force = true) },
                                )
                                noPrices -> MutedText("A carregar os preços…")
                                location == null && vm.locationError != null -> ErrorRetry(
                                    vm.locationError ?: "",
                                    onRetry = { vm.refresh() },
                                )
                                location == null -> MutedText("A procurar onde estás…")
                                listed.isEmpty() -> MutedText(
                                    if (sort == SORT_FAVORITES) "Ainda não tens favoritos. Abre um posto e toca na estrela."
                                    else "Não há postos com ${fuel.short} a menos de ${vm.radiusKm} km. Aumenta o raio.",
                                )
                                else -> listed.forEachIndexed { i, s ->
                                    if (i > 0) HorizontalDivider(color = Atesta.colors.cardBorder)
                                    StationRow(s.station, s.km, onClick = { onOpenStation(s.station.id) }) {
                                        PriceChip(s.station.price, big = true)
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
private fun MutedText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
}

/** Aviso de error con la causa y cómo arreglarlo. */
@Composable
fun ErrorRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
        }
        OutlinedButton(onClick = onRetry) { Text("Tentar de novo") }
    }
}

@Composable
private fun MapButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Atesta.colors.floating,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description) }
    }
}

/** Pastilla con menú desplegable. [label] dice a TalkBack qué se elige («Raio: 10 km»). */
@Composable
fun DropPill(
    text: String,
    options: List<String>,
    selectedIndex: Int,
    selected: Boolean = false,
    label: String? = null,
    onPick: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val c = MaterialTheme.colorScheme
    Box {
        FilterPill(text, selected = selected, onClick = { open = true }, description = label?.let { "$it: $text" }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = if (selected) c.onPrimaryContainer else c.onSurface, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(
                    text = { Text(o, fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal, color = if (i == selectedIndex) c.primary else c.onSurface) },
                    onClick = { open = false; onPick(i) },
                )
            }
        }
    }
}

/** Clic sin efecto de onda (solo para superficies que ya tienen su propio feedback). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
