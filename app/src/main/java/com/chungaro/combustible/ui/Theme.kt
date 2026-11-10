package com.chungaro.combustible.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta propia: verde oscuro + ámbar. No se usan los colores dinámicos del
// sistema para que la app tenga siempre la misma identidad.
private val Light = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF172554),
    // Un poco más oscuro que las tarjetas blancas, para que las secciones se distingan.
    background = Color(0xFFEDF2EE),
    onBackground = Color(0xFF0F1F1D),
    surface = Color(0xFFF4F7F4),
    onSurface = Color(0xFF0F1F1D),
    surfaceVariant = Color(0xFFDDE8E6),
    onSurfaceVariant = Color(0xFF4B5F5C),
    outline = Color(0xFF7C908D),
    outlineVariant = Color(0xFFC6D4D1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFA),
    surfaceContainer = Color(0xFFEDF3EE),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF86EFAC),
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3B2300),
    secondaryContainer = Color(0xFF5C3B06),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF93C5FD),
    onTertiary = Color(0xFF0B2A5B),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0A1312),
    onBackground = Color(0xFFE2ECEA),
    surface = Color(0xFF0A1312),
    onSurface = Color(0xFFE2ECEA),
    surfaceVariant = Color(0xFF26332F),
    onSurfaceVariant = Color(0xFFA7B8B5),
    outline = Color(0xFF718481),
    outlineVariant = Color(0xFF34433F),
    surfaceContainerLowest = Color(0xFF060D0C),
    surfaceContainerLow = Color(0xFF111C1A),
    surfaceContainer = Color(0xFF13201E),
    surfaceContainerHigh = Color(0xFF182826),
    surfaceContainerHighest = Color(0xFF1C2D2B),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/** Verde oscuro de la marca: cabeceras, barras superiores y menú. */
val AtestaGreen = Color(0xFF14532D)
/** Verde claro para resaltar sobre el verde oscuro. */
val AtestaGreenLight = Color(0xFF86EFAC)

/** Letra algo más pequeña que la de Material por defecto, para que quepa más información. */
private val AppTypography: Typography = Typography().let { t ->
    fun TextStyle.small() = copy(fontSize = fontSize * 0.88f, lineHeight = lineHeight * 0.88f)
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
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
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
