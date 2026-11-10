package com.chungaro.combustible.nearby

import android.content.Context
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(vm: NearbyViewModel, defaultFuelId: Int, bottomBar: @Composable () -> Unit) {
    val context = LocalContext.current
    var fuelId by rememberSaveable { mutableIntStateOf(defaultFuelId) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var recenter by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (!vm.started && LocationHelper.hasPermission(context)) vm.refresh() }

    val location = vm.location
    val rowState = vm.rows[fuelId]
    val stations = remember(rowState, location, vm.radiusKm) {
        val all = vm.stationsOf(fuelId).orEmpty()
        val (lat, lon) = location ?: return@remember emptyList<StationAtDistance>()
        all.map { StationAtDistance(it, distanceKm(lat, lon, it.lat, it.lon)) }
            .filter { it.km <= vm.radiusKm * 1.5 }
    }

    Scaffold(bottomBar = bottomBar) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OsmMap(location, stations, recenter, onSelect = { selectedId = it })

            Card(
                Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Todos los combustibles a la vista, sin deslizar
                    FuelSelector(
                        selectedId = fuelId,
                        onSelect = { fuelId = it; selectedId = null },
                        hint = "Escolhe o tipo de combustível para veres a melhor opção perto de ti",
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Box(Modifier.padding(horizontal = 14.dp)) {
                        when {
                            location == null -> Text("Ativa a localização no Início para veres o mapa de preços.", style = MaterialTheme.typography.bodySmall)
                            stations.isEmpty() -> Text("A carregar preços…", style = MaterialTheme.typography.bodySmall)
                            else -> PriceLegend(stations.minOf { it.station.price }, stations.maxOf { it.station.price }, stations.size)
                        }
                    }
                }
            }

            // Botón para volver a la ubicación actual
            if (LocationHelper.hasPermission(context)) {
                SmallFloatingActionButton(
                    onClick = { vm.refresh(); recenter++ },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                        .size(48.dp),
                ) {
                    Icon(painterResource(com.chungaro.combustible.R.drawable.ic_my_location), contentDescription = "Centrar na minha localização")
                }
            }

            selectedId?.let { id ->
                StationSheet(vm, id, fuelId, onDismiss = { selectedId = null })
            }

            if (location != null && stations.isEmpty() && rowState !is FuelRowState.Ready && rowState !is FuelRowState.Error) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

        }
    }
}

@Composable
private fun OsmMap(location: Pair<Double, Double>?, stations: List<StationAtDistance>, recenter: Int, onSelect: (Long) -> Unit) {
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
        }
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
        textSize = 13 * d
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
