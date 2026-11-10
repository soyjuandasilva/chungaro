package com.chungaro.combustible.nearby

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(vm: NearbyViewModel, defaultFuelId: Int, tankLiters: Double, bottomBar: @Composable () -> Unit) {
    val context = LocalContext.current
    var fuelId by rememberSaveable { mutableIntStateOf(defaultFuelId) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { if (!vm.started && LocationHelper.hasPermission(context)) vm.refresh() }

    val location = vm.location
    val rowState = vm.rows[fuelId]
    val stations = remember(rowState, location, vm.radiusKm) {
        val all = vm.stationsOf(fuelId).orEmpty()
        val (lat, lon) = location ?: return@remember emptyList<StationAtDistance>()
        all.map { StationAtDistance(it, distanceKm(lat, lon, it.lat, it.lon)) }
            .filter { it.km <= vm.radiusKm * 1.5 }
    }
    val inRadius = stations.filter { it.km <= vm.radiusKm }

    Scaffold(bottomBar = bottomBar) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OsmMap(location, stations, onSelect = { selectedId = it })

            Card(Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FUEL_TYPES.forEach { f ->
                            FilterChip(
                                selected = f.id == fuelId,
                                onClick = { fuelId = f.id; selectedId = null },
                                label = { Text(f.short) },
                            )
                        }
                    }
                    Text(
                        when {
                            location == null -> "Ative a localização no Início para ver o mapa de preços."
                            stations.isEmpty() && rowState !is FuelRowState.Ready -> "A carregar preços…"
                            else -> "${stations.size} bombas · verde = mais barata, vermelho = mais cara"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            selectedId?.let { id ->
                StationSheet(vm, id, fuelId, onDismiss = { selectedId = null })
            }

            if (location != null && stations.isEmpty() && rowState !is FuelRowState.Ready && rowState !is FuelRowState.Error) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            if (inRadius.size >= 2) {
                val min = inRadius.minOf { it.station.price }
                val max = inRadius.maxOf { it.station.price }
                Card(Modifier.fillMaxWidth().padding(12.dp).align(Alignment.BottomCenter)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🐷", style = MaterialTheme.typography.headlineMedium)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Poupança por depósito", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "↓ ${formatEuro(min)}   ↑ ${formatEuro(max)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "${inRadius.size} bombas a ≤ ${vm.radiusKm} km · ${tankLiters.toInt()} L",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            formatMoney((max - min) * tankLiters),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = androidx.compose.ui.graphics.Color(0xFF16A34A),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMap(location: Pair<Double, Double>?, stations: List<StationAtDistance>, onSelect: (Long) -> Unit) {
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
                    icon = BitmapDrawable(map.resources, priceBubble(map.context, s.station.price, priceColor(t), s.station.brand))
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

private fun priceBubble(context: Context, price: Double, color: Int, brand: String): Bitmap {
    val d = context.resources.displayMetrics.density
    val text = String.format(Locale.getDefault(), "%.3f", price)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12 * d
        typeface = Typeface.DEFAULT_BOLD
        this.color = android.graphics.Color.WHITE
    }
    val style = brandStyle(brand)
    val padH = 6 * d
    val padV = 4 * d
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
    c.drawCircle(cx, cy, badge / 2 - 1.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = style.background.toInt() })
    val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = style.foreground.toInt()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = badge * if (style.label.length > 1) 0.42f else 0.55f
    }
    c.drawText(style.label, cx, cy - (lp.descent() + lp.ascent()) / 2, lp)
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
