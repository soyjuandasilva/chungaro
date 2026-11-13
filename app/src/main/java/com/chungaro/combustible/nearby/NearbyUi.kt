package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.AbasteceHoje
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.Icons
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/** Litros usados para mostrar cuánto se ahorra en un depósito. */
const val TANK_LITERS = 50

/** Color del combustible para el tema actual (tono profundo en el claro). */
val FuelType.uiColor: Color
    @Composable @androidx.compose.runtime.ReadOnlyComposable
    get() = Color(if (com.chungaro.combustible.ui.AbasteceHoje.colors.isDark) color else lightColor)

/** Abre un enlace en el navegador (si no hay ninguno, no hace nada). */
internal fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Sin navegador: no hay nada que abrir.
    }
}

fun stationTitle(s: PricedStation): String =
    // La insignia ya muestra la marca: aquí solo el nombre de la bomba.
    s.name.ifBlank { s.brand }.ifBlank { "Posto ${s.id}" }

fun formatEuro(v: Double): String = String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f €", v)
fun formatMoney(v: Double): String = String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.2f €", v)
fun formatKm(v: Double): String =
    if (v < 1) String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.0f m", v * 1000)
    else String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.1f km", v)

fun openMaps(context: Context, s: PricedStation) {
    val label = Uri.encode(stationTitle(s))
    val uri = Uri.parse("geo:${s.lat},${s.lon}?q=${s.lat},${s.lon}($label)")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${s.lat},${s.lon}")),
        )
    }
}
