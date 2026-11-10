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

@Composable
internal fun OsmMap(
    location: Pair<Double, Double>?,
    stations: List<StationAtDistance>,
    recenter: Int,
    centerOffsetY: Int = 0,
    topo: Boolean = false,
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
        // Capa: callejero normal o relieve
        val source = if (topo) TileSourceFactory.OpenTopo else TileSourceFactory.MAPNIK
        if (map.tileProvider.tileSource != source) map.setTileSource(source)
        map.overlays.clear()
        // Las más baratas se dibujan al final para quedar encima.
        stations.sortedByDescending { it.station.price }.forEach { s ->
            map.overlays.add(
                Marker(map).apply {
                    position = GeoPoint(s.station.lat, s.station.lon)
                    icon = BitmapDrawable(map.resources, priceBubble(map.context, s.station.price, pinColor(s.station.brand), s.station.brand, logos))
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

/** Color del marcador: el de la marca; gris oscuro si la marca es blanca o muy clara. */
internal fun pinColor(brand: String): Int {
    val c = brandStyle(brand).background.toInt()
    val r = (c shr 16) and 0xFF
    val g = (c shr 8) and 0xFF
    val b = c and 0xFF
    val light = (0.299 * r + 0.587 * g + 0.114 * b) > 200
    return if (light) 0xFF374151.toInt() else c
}

/**
 * Marcador: logotipo en un círculo blanco arriba, el precio en una etiqueta
 * del color de la marca y un pico abajo.
 */
private fun priceBubble(context: Context, price: Double, color: Int, brand: String, logos: Map<String, Bitmap>): Bitmap {
    val d = context.resources.displayMetrics.density
    val text = String.format(Locale.getDefault(), "%.3f€", price)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13 * d
        typeface = Typeface.DEFAULT_BOLD
        this.color = android.graphics.Color.WHITE
    }
    val style = brandStyle(brand)
    val padH = 8 * d
    val badge = 30 * d
    val overlap = badge * 0.45f
    val labelH = overlap + paint.textSize + 9 * d
    val tail = 7 * d
    val w = maxOf(paint.measureText(text) + padH * 2, badge + 8 * d)
    val top = badge - overlap
    val h = top + labelH + tail
    val bmp = Bitmap.createBitmap(w.toInt() + 2, h.toInt() + 2, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        this.style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
    }
    // Etiqueta del precio
    val rect = RectF(1f, top, w, top + labelH)
    c.drawRoundRect(rect, 8 * d, 8 * d, bg)
    c.drawRoundRect(rect, 8 * d, 8 * d, border)
    val path = android.graphics.Path().apply {
        moveTo(w / 2 - tail, top + labelH - 1)
        lineTo(w / 2, top + labelH + tail)
        lineTo(w / 2 + tail, top + labelH - 1)
        close()
    }
    c.drawPath(path, bg)
    c.drawText(text, (w - paint.measureText(text)) / 2 + 1, top + overlap + 3 * d + paint.textSize * 0.9f, paint)
    // Logotipo arriba, centrado, en un círculo blanco
    val cx = w / 2
    val cy = badge / 2 + 1
    c.drawCircle(cx, cy, badge / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    c.drawCircle(cx, cy, badge / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
    })
    val logo = localLogo(context, style.logo) ?: style.domain?.let { logos[it] }
    if (logo != null) {
        val r = badge / 2 - 4 * d
        c.drawBitmap(logo, null, RectF(cx - r, cy - r, cx + r, cy + r), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else {
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = badge * if (style.label.length > 1) 0.38f else 0.5f
        }
        c.drawText(style.label, cx, cy - (lp.descent() + lp.ascent()) / 2, lp)
    }
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
