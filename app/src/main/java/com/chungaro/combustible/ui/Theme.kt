package com.chungaro.combustible.ui

import android.app.Activity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Tema oscuro de Atesta (verde casi negro con acentos verde brillante). Se usa
// siempre, con el móvil en modo claro u oscuro, para mantener la identidad.
private val Dark = darkColorScheme(
    primary = Color(0xFF22C55E),
    onPrimary = Color(0xFF04140B),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3B2300),
    secondaryContainer = Color(0xFF5C3B06),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B2A5B),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF071610),
    onBackground = Color(0xFFF1F5F2),
    surface = Color(0xFF071610),
    onSurface = Color(0xFFF1F5F2),
    surfaceVariant = Color(0xFF173A29),
    onSurfaceVariant = Color(0xFFA7BEB0),
    outline = Color(0xFF2F5C45),
    outlineVariant = Color(0xFF1C3D2D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    inverseSurface = Color(0xFFE7F2EA),
    inverseOnSurface = Color(0xFF0B1F15),
    surfaceContainerLowest = Color(0xFF040E0A),
    surfaceContainerLow = Color(0xFF0A1D15),
    surfaceContainer = Color(0xFF0D2419),
    surfaceContainerHigh = Color(0xFF102B1E),
    surfaceContainerHighest = Color(0xFF123224),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/** Fondo de cabeceras y barras: el mismo verde casi negro del fondo. */
val AtestaGreen = Color(0xFF071610)
/** Verde brillante de acento (botones, pestaña activa, precios). */
val AtestaGreenLight = Color(0xFF22C55E)
/** Tarjetas sobre el fondo oscuro. */
val AtestaCard = Color(0xFF0E2A1D)
/** Borde fino de las tarjetas. */
val AtestaBorder = Color(0xFF1E4A34)

/**
 * Barlow Semi Condensed (descargada por GitHub Actions a res/font). Si no está,
 * null y se usa la letra del sistema.
 */
@Composable
private fun appFontFamily(): FontFamily? {
    val context = LocalContext.current
    return remember {
        fun id(name: String) = context.resources.getIdentifier(name, "font", context.packageName)
        val fonts = listOf(
            "barlow_medium" to FontWeight.Medium,
            "barlow_semibold" to FontWeight.SemiBold,
            "barlow_bold" to FontWeight.Bold,
            "barlow_extrabold" to FontWeight.ExtraBold,
        ).mapNotNull { (n, w) -> id(n).takeIf { it != 0 }?.let { Font(it, w) } }
        // La medium también sirve de normal
        val normal = id("barlow_medium").takeIf { it != 0 }?.let { Font(it, FontWeight.Normal) }
        if (fonts.isEmpty()) null else FontFamily(listOfNotNull(normal) + fonts)
    }
}

/** Letra algo más pequeña que la de Material por defecto, para que quepa más información. */
private fun appTypography(family: FontFamily?): Typography = Typography().let { t ->
    // La condensada es más estrecha: se mantiene el tamaño de Material.
    val scale = if (family != null) 1.0f else 0.88f
    fun TextStyle.small() = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontFamily = family ?: fontFamily,
    )
    t.copy(
        displayLarge = t.displayLarge.small(), displayMedium = t.displayMedium.small(), displaySmall = t.displaySmall.small(),
        headlineLarge = t.headlineLarge.small(), headlineMedium = t.headlineMedium.small(), headlineSmall = t.headlineSmall.small(),
        titleLarge = t.titleLarge.small(), titleMedium = t.titleMedium.small(), titleSmall = t.titleSmall.small(),
        bodyLarge = t.bodyLarge.small(), bodyMedium = t.bodyMedium.small(), bodySmall = t.bodySmall.small(),
        labelLarge = t.labelLarge.small(), labelMedium = t.labelMedium.small(), labelSmall = t.labelSmall.small(),
    )
}

@Composable
fun ChungaroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Dark,
        shapes = AppShapes,
        typography = appTypography(appFontFamily()),
    ) {
        // Texto e iconos claros por defecto: sobre los fondos con degradado no hay
        // una superficie que lo indique y quedarían en negro.
        CompositionLocalProvider(LocalContentColor provides Dark.onBackground, content = content)
    }
}

/** Barra superior verde oscuro, igual en todas las pantallas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun atestaTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = AtestaGreen,
    scrolledContainerColor = AtestaGreen,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White,
)

/**
 * Iconos de la barra de estado: claros sobre fondo oscuro ([darkBackground] = true)
 * u oscuros sobre fondo claro (el mapa).
 */
@Composable
fun StatusBarIcons(darkBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkBackground
    }
}

/** Fondo de la app con profundidad: verde que se oscurece hacia abajo. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xFF0D3322), Color(0xFF07190F), Color(0xFF030B07)),
)

fun Modifier.appBackground(): Modifier = this.background(AppBackgroundBrush)
