package com.chungaro.combustible.nearby

import android.content.Context
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import androidx.compose.runtime.mutableStateMapOf
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val SORT_CHEAPEST = 0
private const val SORT_NEAREST = 1
private const val SORT_FAVORITES = 2
private const val PAGE = 10

/**
 * Inicio: mapa a pantalla completa con los precios y, abajo, una hoja que se
 * arrastra con el combustible, el orden y la lista de bombas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    vm: NearbyViewModel,
    defaultFuelId: Int,
    tankLiters: Double,
    favorites: List<Long>,
    onToggleFavorite: (Long) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenNews: () -> Unit = {},
) {
    com.chungaro.combustible.ui.StatusBarIcons(darkBackground = false)
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
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

    var fuelId by rememberSaveable(defaultFuelId) { mutableIntStateOf(defaultFuelId) }
    var sort by rememberSaveable { mutableIntStateOf(SORT_CHEAPEST) }
    var shown by rememberSaveable { mutableIntStateOf(PAGE) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recenter by remember { mutableIntStateOf(0) }

    val location = vm.location
    val rowState = vm.rows[fuelId]
    val all = remember(rowState, location, vm.pricesVersion, fuelId) {
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
    }
    val minP = inRadius.minOfOrNull { it.station.price }
    val maxP = inRadius.maxOfOrNull { it.station.price }

    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val peek = screenH * 0.45f
    val peekPx = with(LocalDensity.current) { peek.roundToPx() }
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peek,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetDragHandle = null,
        sheetShadowElevation = 12.dp,
        sheetContent = {
            // Al subir la hoja del todo, la cabecera verde no queda bajo la barra de estado.
            Box(Modifier.fillMaxWidth().background(HeaderDark)) { SheetStatusBarSpacer(scaffoldState.bottomSheetState) }
            SheetHeader(
                fuelId = fuelId,
                onFuel = { fuelId = it; shown = PAGE; selectedId = null },
                sort = sort,
                onSort = { sort = it; shown = PAGE },
                radiusKm = vm.radiusKm,
                onRadius = { vm.selectRadius(it) },
            )
            LazyColumn(
                Modifier.fillMaxWidth().height(screenH * 0.85f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // «Abastece hoje ou espera por segunda?» para el combustible elegido
                if (sort != SORT_FAVORITES) {
                    item(key = "monday") { MondayCard(vm, FUEL_TYPES.first { it.id == fuelId }, onOpenNews) }
                }
                when {
                    !hasPermission -> item {
                        PermissionCard {
                            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                    location == null || (all.isEmpty() && rowState !is FuelRowState.Error) -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    else -> {
                        if (sort != SORT_FAVORITES && inRadius.size >= 2 && minP != null && maxP != null) {
                            item { SavingsCard(minP, maxP, inRadius.size, tankLiters) }
                        }
                        if (listed.isEmpty()) {
                            item {
                                Text(
                                    if (sort == SORT_FAVORITES) "Ainda não tens bombas favoritas. Abre uma bomba e toca no ♥ para a guardares."
                                    else "Não há bombas com este combustível a menos de ${vm.radiusKm} km. Experimenta um raio maior.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        } else {
                            item {
                                StationList(listed.take(shown), minP, maxP) { selectedId = it }
                            }
                            if (listed.size > shown) {
                                item {
                                    TextButton(onClick = { shown += PAGE }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Mostrar mais bombas", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Preços oficiais da DGEG — precoscombustiveis.dgeg.gov.pt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            OsmMap(location, onMap, recenter, centerOffsetY = -peekPx / 2, onSelect = { selectedId = it })

            // Menú
            Surface(
                onClick = onOpenMenu,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp,
                modifier = Modifier.statusBarsPadding().padding(16.dp).size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Menu, contentDescription = "Menu") }
            }

            // Centrar en mi ubicación, justo encima de la hoja
            if (hasPermission) {
                Surface(
                    onClick = { vm.refresh(); recenter++ },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = peek + 16.dp)
                        .size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(com.chungaro.combustible.R.drawable.ic_my_location), contentDescription = "Centrar na minha localização")
                    }
                }
            }
        }
    }

    selectedId?.let { id ->
        StationSheet(
            vm, id, fuelId,
            onDismiss = { selectedId = null },
            isFavorite = id in favorites,
            onToggleFavorite = { onToggleFavorite(id) },
        )
    }
}

/** Cabecera oscura de la hoja: combustible, radio y orden. */
@Composable
private fun SheetHeader(
    fuelId: Int,
    onFuel: (Int) -> Unit,
    sort: Int,
    onSort: (Int) -> Unit,
    radiusKm: Int,
    onRadius: (Int) -> Unit,
) {
    val fuel = FUEL_TYPES.first { it.id == fuelId }
    Surface(color = HeaderDark, contentColor = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.4f), modifier = Modifier.width(40.dp).height(5.dp)) {}
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Combustível", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    var open by remember { mutableStateOf(false) }
                    Box {
                        Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = fuel.uiColor) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(4.dp).height(18.dp).background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
                                Text("  " + fuel.short, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Escolher combustível", tint = Color.White)
                            }
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            FUEL_TYPES.forEach { f ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            f.short,
                                            fontWeight = if (f.id == fuelId) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (f.id == fuelId) f.uiColor else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    leadingIcon = { Box(Modifier.width(4.dp).height(20.dp).background(f.uiColor, RoundedCornerShape(2.dp))) },
                                    onClick = { open = false; onFuel(f.id) },
                                )
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Raio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    var open by remember { mutableStateOf(false) }
                    Box {
                        Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("≤ $radiusKm km", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Escolher raio")
                            }
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            NearbyViewModel.RADIUS_OPTIONS.forEach { km ->
                                DropdownMenuItem(
                                    text = { Text("$km km", fontWeight = if (km == radiusKm) FontWeight.ExtraBold else FontWeight.Normal) },
                                    onClick = { open = false; onRadius(km) },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Ordenar bombas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(4.dp)) {
                    listOf("Mais barata", "Mais próxima", "Favoritas").forEachIndexed { i, label ->
                        val sel = i == sort
                        Surface(
                            onClick = { onSort(i) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) Color.White else Color.Transparent,
                            contentColor = if (sel) HeaderDark else Color.White,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Cuánto se ahorra al llenar en la más barata frente a la más cara de la zona. */
@Composable
private fun SavingsCard(min: Double, max: Double, count: Int, liters: Double) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("💶", style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Poupança por depósito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "↓ ${formatEuro(min)}   ↑ ${formatEuro(max)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("$count bombas · ${liters.toInt()} L", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                formatMoney((max - min) * liters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF16A34A),
            )
        }
    }
}

/** Lista de bombas: logotipo, nombre, localidad · morada, distancia, precio y cuándo se actualizó. */
@Composable
private fun StationList(items: List<StationAtDistance>, min: Double?, max: Double?, onOpen: (Long) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 2.dp) {
        Column {
            items.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                val st = s.station
                val priceColor = when {
                    min != null && max != null && max - min > 1e-6 && st.price <= min + 1e-6 -> Color(0xFF16A34A)
                    min != null && max != null && max - min > 1e-6 && st.price >= max - 1e-6 -> Color(0xFFDC2626)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(st.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandBadge(st.brand, size = 48.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(stationTitle(st), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOf(st.locality, st.address).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatKm(s.km), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                String.format(Locale.getDefault(), "%.3f", st.price),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = priceColor,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text("€", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = priceColor, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        if (st.updated.isNotBlank()) {
                            Text(updatedText(st.updated), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMap(
    location: Pair<Double, Double>?,
    stations: List<StationAtDistance>,
    recenter: Int,
    centerOffsetY: Int = 0,
    onSelect: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
        }.also { it.setMapCenterOffset(0, centerOffsetY) }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }
    var centered by remember { mutableStateOf(false) }
    // Al pulsar «centrar»: ir a la ubicación (y otra vez cuando llegue la nueva).
    LaunchedEffect(recenter, location) {
        val loc = location ?: return@LaunchedEffect
        if (recenter > 0) mapView.controller.animateTo(GeoPoint(loc.first, loc.second), 14.5, 600L)
    }

    // Logotipos de las marcas presentes, descargados una vez y en caché.
    val logos = remember { mutableStateMapOf<String, Bitmap>() }
    val domains = remember(stations) { stations.mapNotNull { brandStyle(it.station.brand).domain }.toSet() }
    LaunchedEffect(domains) {
        domains.filter { it !in logos }.forEach { domain ->
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context).data(brandLogoUrl(domain)).size(96).allowHardware(false).build(),
            )
            ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap?.let { logos[domain] = it }
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(), update = { map ->
        map.overlays.clear()
        val prices = stations.map { it.station.price }
        val min = prices.minOrNull() ?: 0.0
        val max = prices.maxOrNull() ?: 0.0
        // Las más baratas se dibujan al final para quedar encima.
        stations.sortedByDescending { it.station.price }.forEach { s ->
            val t = if (max - min < 1e-6) 0.0 else (s.station.price - min) / (max - min)
            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(s.station.lat, s.station.lon)
                    icon = BitmapDrawable(map.resources, priceBubble(map.context, s.station.price, priceColor(t), s.station.brand, logos))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> onSelect(s.station.id); true }
                },
            )
        }
        location?.let { (lat, lon) ->
            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    icon = BitmapDrawable(map.resources, meDot(map.context))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ -> true }
                },
            )
            if (!centered) {
                map.controller.setCenter(GeoPoint(lat, lon))
                centered = true
            }
        }
        map.invalidate()
    })
}

/** Verde (barata) → ámbar → rojo (cara). */
private fun priceColor(t: Double): Int {
    val stops = intArrayOf(0xFF16A34A.toInt(), 0xFFF59E0B.toInt(), 0xFFDC2626.toInt())
    val x = (t.coerceIn(0.0, 1.0) * 2)
    val i = x.toInt().coerceAtMost(1)
    val f = (x - i).toFloat()
    val a = stops[i]
    val b = stops[i + 1]
    fun ch(c: Int, shift: Int) = (c shr shift) and 0xFF
    fun mix(shift: Int) = (ch(a, shift) + (ch(b, shift) - ch(a, shift)) * f).toInt()
    return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

private fun priceBubble(context: Context, price: Double, color: Int, brand: String, logos: Map<String, Bitmap>): Bitmap {
    val d = context.resources.displayMetrics.density
    val text = String.format(Locale.getDefault(), "%.3f", price)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12 * d
        typeface = Typeface.DEFAULT_BOLD
        this.color = android.graphics.Color.WHITE
    }
    val style = brandStyle(brand)
    val padH = 7 * d
    val padV = 6 * d
    val tail = 6 * d
    val h = paint.textSize + padV * 2
    val badge = h - 4 * d
    val w = badge + 4 * d + paint.measureText(text) + padH * 2
    val bmp = Bitmap.createBitmap(w.toInt() + 2, (h + tail).toInt() + 2, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    c.drawRoundRect(RectF(1f, 1f, w, h), h / 2, h / 2, bg)
    val path = android.graphics.Path().apply {
        moveTo(w / 2 - tail, h - 1)
        lineTo(w / 2, h + tail)
        lineTo(w / 2 + tail, h - 1)
        close()
    }
    c.drawPath(path, bg)
    // Círculo de la marca a la izquierda.
    val cx = 1 + 2 * d + badge / 2
    val cy = 1 + h / 2
    c.drawCircle(cx, cy, badge / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    val logo = localLogo(context, style.logo) ?: style.domain?.let { logos[it] }
    if (logo != null) {
        val r = badge / 2 - 2.5f * d
        c.drawBitmap(logo, null, RectF(cx - r, cy - r, cx + r, cy + r), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else {
        c.drawCircle(cx, cy, badge / 2 - 1.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = style.background.toInt() })
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = style.foreground.toInt()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = badge * if (style.label.length > 1) 0.42f else 0.55f
        }
        c.drawText(style.label, cx, cy - (lp.descent() + lp.ascent()) / 2, lp)
    }
    c.drawText(text, 1 + 2 * d + badge + 4 * d, padV + paint.textSize * 0.85f + 1, paint)
    return bmp
}

private fun meDot(context: Context): Bitmap {
    val d = context.resources.displayMetrics.density
    val size = (20 * d).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE })
    c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2563EB.toInt() })
    return bmp
}

@Composable
private fun PriceLegend(min: Double, max: Double, count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatEuro(min), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF16A34A))
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color(0xFF16A34A),
                                androidx.compose.ui.graphics.Color(0xFFF59E0B),
                                androidx.compose.ui.graphics.Color(0xFFDC2626),
                            ),
                        ),
                    ),
            )
            Text(formatEuro(max), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFDC2626))
        }
        Row {
            Text("mais barata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("$count bombas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("mais cara", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

private val localLogoCache = HashMap<String, Bitmap?>()

/** Logotipo cuadrado incluido en la app (los anchos, como Intermarché, no caben en la burbuja). */
private fun localLogo(context: Context, key: String?): Bitmap? {
    key ?: return null
    return localLogoCache.getOrPut(key) {
        val id = context.resources.getIdentifier("brand_$key", "drawable", context.packageName)
        if (id == 0) null
        else android.graphics.BitmapFactory.decodeResource(context.resources, id)?.takeIf { it.width <= it.height * 2 }
    }
}
