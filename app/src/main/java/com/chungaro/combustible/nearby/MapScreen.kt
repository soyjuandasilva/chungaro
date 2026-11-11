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
    pastel: Boolean = false,
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
            ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap?.let { logos[domain] = trimLogo(it) }
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(), update = { map ->
        // Capa: callejero normal o relieve
        val source = if (topo) TileSourceFactory.OpenTopo else TileSourceFactory.MAPNIK
        if (map.tileProvider.tileSource != source) map.setTileSource(source)
        // Tonos suaves: menos saturación y algo más claro, para que destaquen los marcadores.
        map.overlayManager.tilesOverlay.setColorFilter(if (pastel) PASTEL_FILTER else null)
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

/** Filtro de color de los mosaicos del mapa: menos saturado y más luminoso. */
private val PASTEL_FILTER = android.graphics.ColorMatrixColorFilter(
    android.graphics.ColorMatrix().apply {
        setSaturation(0.55f)
        postConcat(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    0.88f, 0f, 0f, 0f, 28f,
                    0f, 0.9f, 0f, 0f, 30f,
                    0f, 0f, 0.88f, 0f, 26f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    },
)

/** Mezcla un color con blanco (t > 0) o con negro (t < 0). */
private fun shade(c: Int, t: Float): Int {
    fun ch(shift: Int): Int {
        val v = (c shr shift) and 0xFF
        return if (t >= 0) (v + (255 - v) * t).toInt() else (v * (1 + t)).toInt()
    }
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

/**
 * Marcador con volumen: cuerpo del color de la marca con degradado, borde
 * blanco y sombra; arriba el logotipo en una pastilla blanca y abajo el precio
 * grande; termina en pico.
 */
private fun priceBubble(context: Context, price: Double, color: Int, brand: String, logos: Map<String, Bitmap>): Bitmap {
    val d = context.resources.displayMetrics.density
    val text = String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f€", price)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16 * d
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        this.color = android.graphics.Color.WHITE
        setShadowLayer(2 * d, 0f, 1 * d, 0x55000000)
    }
    val style = brandStyle(brand)
    val shadowPad = 6 * d
    val padH = 10 * d
    val logoH = 26 * d
    val gap = 4 * d
    val textH = paint.textSize * 1.15f
    val bodyW = maxOf(paint.measureText(text) + padH * 2, 72 * d)
    val bodyH = 6 * d + logoH + gap + textH + 6 * d
    val tail = 9 * d
    val w = bodyW + shadowPad * 2
    val h = bodyH + tail + shadowPad * 2
    val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val left = shadowPad
    val top = shadowPad
    val rect = RectF(left, top, left + bodyW, top + bodyH)
    val radius = 12 * d

    // Silueta (cuerpo + pico) con sombra
    val shape = android.graphics.Path().apply {
        addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
        moveTo(left + bodyW / 2 - tail, top + bodyH - 1)
        lineTo(left + bodyW / 2, top + bodyH + tail)
        lineTo(left + bodyW / 2 + tail, top + bodyH - 1)
        close()
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        setShadowLayer(5 * d, 0f, 3 * d, 0x88000000.toInt())
    }
    c.drawPath(shape, shadowPaint)
    // Degradado del color de la marca (más claro arriba)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, top, 0f, top + bodyH + tail, shade(color, 0.22f), shade(color, -0.25f), android.graphics.Shader.TileMode.CLAMP)
    }
    c.drawPath(shape, fill)
    // Brillo en la mitad superior
    val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, top, 0f, top + bodyH / 2, 0x40FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP)
    }
    c.drawRoundRect(RectF(left, top, left + bodyW, top + bodyH / 2), radius, radius, gloss)
    // Borde blanco
    c.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        this.style = Paint.Style.STROKE
        strokeWidth = 2 * d
    })

    // Pastilla blanca con el logotipo
    val logoRect = RectF(left + 6 * d, top + 6 * d, left + bodyW - 6 * d, top + 6 * d + logoH)
    c.drawRoundRect(logoRect, 7 * d, 7 * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    val logo = localLogo(context, style.logo) ?: style.domain?.let { logos[it] }
    if (logo != null) {
        // Encaja el logotipo manteniendo su proporción
        val availH = logoH - 3 * d
        val availW = logoRect.width() - 6 * d
        val ratio = logo.width.toFloat() / logo.height.coerceAtLeast(1)
        var lw = availH * ratio
        var lh = availH
        if (lw > availW) { lw = availW; lh = availW / ratio }
        val cx = logoRect.centerX()
        val cy = logoRect.centerY()
        c.drawBitmap(logo, null, RectF(cx - lw / 2, cy - lh / 2, cx + lw / 2, cy + lh / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else {
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = logoH * 0.62f
        }
        val label = if (brand.isNotBlank() && brand.length <= 12) brand.uppercase() else style.label
        while (lp.measureText(label) > logoRect.width() - 8 * d && lp.textSize > 6 * d) lp.textSize *= 0.9f
        c.drawText(label, logoRect.centerX(), logoRect.centerY() - (lp.descent() + lp.ascent()) / 2, lp)
    }
    // Precio
    c.drawText(text, left + (bodyW - paint.measureText(text)) / 2, logoRect.bottom + gap + paint.textSize * 0.95f, paint)
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
        if (id == 0) null else android.graphics.BitmapFactory.decodeResource(context.resources, id)?.let { trimLogo(it) }
    }
}

/**
 * Recorta el margen blanco o transparente alrededor de un logotipo para que
 * ocupe toda la pastilla del marcador (algunos traen mucho aire y se ven diminutos).
 */
internal fun trimLogo(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    if (w < 4 || h < 4) return src
    val px = IntArray(w * h)
    src.getPixels(px, 0, w, 0, 0, w, h)
    fun empty(c: Int): Boolean {
        val a = (c ushr 24) and 0xFF
        if (a < 24) return true
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return r > 238 && g > 238 && b > 238
    }
    var minX = w; var minY = h; var maxX = -1; var maxY = -1
    for (y in 0 until h) for (x in 0 until w) {
        if (!empty(px[y * w + x])) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
    }
    if (maxX < minX || maxY < minY) return src
    val pad = (maxOf(maxX - minX, maxY - minY) * 0.04f).toInt()
    minX = (minX - pad).coerceAtLeast(0); minY = (minY - pad).coerceAtLeast(0)
    maxX = (maxX + pad).coerceAtMost(w - 1); maxY = (maxY + pad).coerceAtMost(h - 1)
    return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1)
}
