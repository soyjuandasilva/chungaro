package com.chungaro.combustible.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta propia: verde petróleo + ámbar. No se usan los colores dinámicos del
// sistema para que la app tenga siempre la misma identidad.
private val Light = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF172554),
    // Un poco más oscuro que las tarjetas blancas, para que las secciones se distingan.
    background = Color(0xFFE4ECEA),
    onBackground = Color(0xFF0F1F1D),
    surface = Color(0xFFF1F6F5),
    onSurface = Color(0xFF0F1F1D),
    surfaceVariant = Color(0xFFDDE8E6),
    onSurfaceVariant = Color(0xFF4B5F5C),
    outline = Color(0xFF7C908D),
    outlineVariant = Color(0xFFC6D4D1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFA),
    surfaceContainer = Color(0xFFE8F1EF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
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

@Composable
fun ChungaroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}
