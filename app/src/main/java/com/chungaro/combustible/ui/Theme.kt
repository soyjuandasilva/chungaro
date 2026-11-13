package com.chungaro.combustible.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/*
 * Sistema de color de Abastece Hoje.
 *
 * Dos temas con la misma identidad (verde de combustible sobre neutros verdosos):
 * - Escuro: verde casi negro, para la noche y el interior del coche.
 * - Claro: fondo casi blanco, para leer los precios a pleno sol en la bomba.
 * Por defecto sigue al sistema; se puede fijar en Definições → Aparência.
 *
 * Reglas: todo color sale de aquí (MaterialTheme.colorScheme o AbasteceHoje.colors).
 * Texto ≥ 4,5:1 sobre su fondo; iconos, bordes de controles y foco ≥ 3:1.
 */

/** Valores guardados del ajuste de tema. */
object ThemeMode {
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val DARK = "dark"
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF22C55E),
    onPrimary = Color(0xFF04210F),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFDCFCE7),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF3B2300),
    secondaryContainer = Color(0xFF4A3205),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B2A5B),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF08130E),
    onBackground = Color(0xFFEEF4F0),
    surface = Color(0xFF08130E),
    onSurface = Color(0xFFEEF4F0),
    surfaceVariant = Color(0xFF16301F),
    onSurfaceVariant = Color(0xFFA3B8AB),
    surfaceTint = Color(0xFF22C55E),
    outline = Color(0xFF5E8270),
    outlineVariant = Color(0xFF23402F),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3B1212),
    onErrorContainer = Color(0xFFFECACA),
    inverseSurface = Color(0xFFE7F2EA),
    inverseOnSurface = Color(0xFF0B1F15),
    inversePrimary = Color(0xFF15803D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF1C3729),
    surfaceDim = Color(0xFF08130E),
    surfaceContainerLowest = Color(0xFF050D09),
    surfaceContainerLow = Color(0xFF0C1B14),
    surfaceContainer = Color(0xFF0F2018),
    surfaceContainerHigh = Color(0xFF16301F),
    surfaceContainerHighest = Color(0xFF1C3729),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFFB45309),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF1D4ED8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF172554),
    background = Color(0xFFF3F6F4),
    onBackground = Color(0xFF0F1A14),
    surface = Color(0xFFF3F6F4),
    onSurface = Color(0xFF0F1A14),
    surfaceVariant = Color(0xFFE6EEE9),
    onSurfaceVariant = Color(0xFF4A5E53),
    surfaceTint = Color(0xFF15803D),
    outline = Color(0xFF74897D),
    outlineVariant = Color(0xFFD8E2DC),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    inverseSurface = Color(0xFF1B2A22),
    inverseOnSurface = Color(0xFFEEF4F0),
    inversePrimary = Color(0xFF4ADE80),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE2E9E5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF3F0),
    surfaceContainerHighest = Color(0xFFE6EEE9),
)

/**
 * Colores con significado propio de Abastece Hoje, además del esquema de Material.
 * Los precios que suben son malos para el conductor (rojo) y los que bajan,
 * buenos (verde); nunca se comunican solo con color: siempre van con flecha o texto.
 */
@Immutable
data class BrandColors(
    val isDark: Boolean,
    /** Iconos y textos de acento sobre las superficies. */
    val accent: Color,
    /** Superficie de las tarjetas. */
    val card: Color,
    /** Borde fino de tarjetas y separadores de listas. */
    val cardBorder: Color,
    /** Relleno de la etiqueta del precio y su texto. */
    val price: Color,
    val onPrice: Color,
    /** Precio a la baja (bueno) y al alza (malo), y sus fondos suaves. */
    val down: Color,
    val downContainer: Color,
    val up: Color,
    val upContainer: Color,
    /** Avisos («não compensa», lunes de espera). */
    val warning: Color,
    val warningContainer: Color,
    /** Estrella de favorito. */
    val star: Color,
    /** Información neutra (mercado, mapa). */
    val info: Color,
    val purple: Color,
    /** Controles flotantes sobre el mapa. */
    val floating: Color,
)

private val DarkBrand = BrandColors(
    isDark = true,
    accent = Color(0xFF4ADE80),
    card = Color(0xFF0F2018),
    cardBorder = Color(0xFF23402F),
    price = Color(0xFF22C55E),
    onPrice = Color(0xFF04210F),
    down = Color(0xFF4ADE80),
    downContainer = Color(0xFF0F3A26),
    up = Color(0xFFF87171),
    upContainer = Color(0xFF3B1212),
    warning = Color(0xFFFBBF24),
    warningContainer = Color(0xFF3A2A06),
    star = Color(0xFFFBBF24),
    info = Color(0xFF60A5FA),
    purple = Color(0xFFC4B5FD),
    floating = Color(0xFF16301F),
)

private val LightBrand = BrandColors(
    isDark = false,
    accent = Color(0xFF15803D),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFD8E2DC),
    price = Color(0xFF15803D),
    onPrice = Color(0xFFFFFFFF),
    down = Color(0xFF15803D),
    downContainer = Color(0xFFDCFCE7),
    up = Color(0xFFB91C1C),
    upContainer = Color(0xFFFEE2E2),
    warning = Color(0xFFB45309),
    warningContainer = Color(0xFFFEF3C7),
    star = Color(0xFFD97706),
    info = Color(0xFF1D4ED8),
    purple = Color(0xFF6D28D9),
    floating = Color(0xFFFFFFFF),
)

val LocalBrandColors = staticCompositionLocalOf { DarkBrand }

/** Acceso a los colores propios: `AbasteceHoje.colors.price`. */
object AbasteceHoje {
    val colors: BrandColors
        @Composable @ReadOnlyComposable
        get() = LocalBrandColors.current
}

// Alias de los nombres antiguos, ahora según el tema.
/** Fondo de pantallas y barras. */
val BrandGreen: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
/** Verde de acento (iconos, pestaña activa, enlaces). */
val BrandGreenLight: Color @Composable @ReadOnlyComposable get() = AbasteceHoje.colors.accent
/** Tarjetas. */
val BrandCard: Color @Composable @ReadOnlyComposable get() = AbasteceHoje.colors.card
/** Borde fino de tarjetas y separadores. */
val BrandBorder: Color @Composable @ReadOnlyComposable get() = AbasteceHoje.colors.cardBorder

/** Texto casi negro para fondos claros. */
val DarkInk = Color(0xFF07130D)

/*
 * Panel de precio de 7 segmentos, como los totens de los postos. Es negro en los dos
 * temas: es un objeto físico, no una superficie de la app.
 */
/** Fondo del panel. */
val SegmentPanel = Color(0xFF050706)
/** Segmentos encendidos (≥ 4,5:1 sobre el panel). */
val SegmentOn = Color(0xFFF4F7F5)
/** Segmentos apagados: se adivinan detrás, como en un display real. */
val SegmentOff = Color(0x14F4F7F5)
/** Texto pequeño dentro del panel (≥ 4,5:1 sobre el panel). */
val SegmentLabel = Color(0xFF9FB0A6)

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Blanco o casi negro, el que más contraste dé sobre [background] (siempre ≥ 4,5:1). */
fun inkOn(background: Color): Color =
    if (contrast(Color.White, background) >= contrast(DarkInk, background)) Color.White else DarkInk

/**
 * Barlow Semi Condensed (descargada por GitHub Actions a res/font). Si no está,
 * null y se usa la letra del sistema al mismo tamaño.
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

/** Tipografía de Material con la familia de la app y cifras tabulares (los precios no bailan). */
private fun appTypography(family: FontFamily?): Typography = Typography().let { t ->
    fun TextStyle.app() = copy(fontFamily = family ?: fontFamily, fontFeatureSettings = "tnum")
    t.copy(
        displayLarge = t.displayLarge.app(), displayMedium = t.displayMedium.app(), displaySmall = t.displaySmall.app(),
        headlineLarge = t.headlineLarge.app(), headlineMedium = t.headlineMedium.app(), headlineSmall = t.headlineSmall.app(),
        titleLarge = t.titleLarge.app(), titleMedium = t.titleMedium.app(), titleSmall = t.titleSmall.app(),
        bodyLarge = t.bodyLarge.app(), bodyMedium = t.bodyMedium.app(), bodySmall = t.bodySmall.app(),
        labelLarge = t.labelLarge.app(), labelMedium = t.labelMedium.app(), labelSmall = t.labelSmall.app(),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

/** Si el tema que toca es oscuro, según el ajuste guardado y el del sistema. */
@Composable
fun isDarkMode(mode: String): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun ChungaroTheme(mode: String = ThemeMode.AUTO, content: @Composable () -> Unit) {
    val dark = isDarkMode(mode)
    val scheme: ColorScheme = if (dark) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        shapes = AppShapes,
        typography = appTypography(appFontFamily()),
    ) {
        CompositionLocalProvider(
            LocalBrandColors provides if (dark) DarkBrand else LightBrand,
            LocalContentColor provides scheme.onBackground,
            content = content,
        )
    }
}

/** Barra superior del color del fondo, igual en todas las pantallas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun abasteceHojeTopBarColors(): TopAppBarColors {
    val c = MaterialTheme.colorScheme
    return TopAppBarDefaults.topAppBarColors(
        containerColor = c.background,
        scrolledContainerColor = c.surfaceContainerHigh,
        titleContentColor = c.onBackground,
        navigationIconContentColor = c.onBackground,
        actionIconContentColor = c.onBackground,
    )
}

/**
 * Iconos de las barras del sistema: claros sobre fondo oscuro ([darkBackground] = true)
 * u oscuros sobre fondo claro. Por defecto, según el tema.
 */
@Composable
fun StatusBarIcons(darkBackground: Boolean = AbasteceHoje.colors.isDark) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkBackground
            isAppearanceLightNavigationBars = !darkBackground
        }
    }
}

/** Fondo de las pantallas: plano, del color del tema. */
@Composable
fun Modifier.appBackground(): Modifier = this.background(MaterialTheme.colorScheme.background)
