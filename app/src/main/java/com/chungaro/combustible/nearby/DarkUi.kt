package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.rememberReduceMotion
import com.chungaro.combustible.ui.Motion
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.ui.APP_LOCALE
import com.chungaro.combustible.ui.AbasteceHoje
import com.chungaro.combustible.ui.StatusBarIcons
import com.chungaro.combustible.ui.abasteceHojeTopBarColors
import com.chungaro.combustible.ui.SegmentDigits
import com.chungaro.combustible.ui.SegmentLabel
import com.chungaro.combustible.ui.SegmentPanel
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Precio con coma decimal y €: "1,659€" (siempre en português de Portugal). */
fun euro3(v: Double): String = String.format(APP_LOCALE, "%.3f€", v)

/** Radio de las tarjetas en toda la app. */
val CardShape = RoundedCornerShape(16.dp)

/**
 * Superficie de tarjeta. Sobre el fondo: color de tarjeta y borde fino, sin sombra.
 * Flotando sobre el mapa ([floating]): sombra suave, sin borde. Nunca las dos cosas.
 */
@Composable
fun Modifier.cardSurface(shape: Shape = CardShape, floating: Boolean = false): Modifier {
    val c = AbasteceHoje.colors
    return if (floating) {
        this.shadow(8.dp, shape).background(c.floating, shape).clip(shape)
    } else {
        this.background(c.card, shape).border(1.dp, c.cardBorder, shape).clip(shape)
    }
}

/** Tarjeta base de todo el diseño. */
@Composable
fun DarkCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().cardSurface(shape), content = content)
}

/**
 * Precio como en los totens de los postos: panel negro, cifras de 7 segmentos y una
 * franja a la izquierda del color del combustible ([color]; sin él, el verde de precio).
 * Con letra grande, las cifras crecen hasta un 40 %.
 */
@Composable
fun PriceChip(price: Double, color: Color? = null, caption: String? = null, big: Boolean = false) {
    val bar = color ?: AbasteceHoje.colors.price
    val shape = RoundedCornerShape(if (big) 9.dp else 7.dp)
    val scale = LocalDensity.current.fontScale.coerceIn(1f, 1.4f)
    val digitHeight = (if (big) 24.dp else 18.dp) * scale
    val spoken = String.format(APP_LOCALE, "%.3f euros por litro", price)
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(SegmentPanel, shape)
            .then(if (AbasteceHoje.colors.isDark) Modifier.border(1.dp, AbasteceHoje.colors.cardBorder, shape) else Modifier)
            .clearAndSetSemantics { contentDescription = caption?.let { "$it, $spoken" } ?: spoken },
    ) {
        Box(Modifier.width(if (big) 5.dp else 4.dp).fillMaxHeight().background(bar))
        Column(
            Modifier.padding(
                start = if (big) 10.dp else 8.dp,
                end = if (big) 11.dp else 9.dp,
                top = if (big) 8.dp else 6.dp,
                bottom = if (caption != null) 4.dp else if (big) 8.dp else 6.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Si el precio cambia, la cifra se desliza: hacia abajo si baja, hacia arriba si sube.
            val reduce = rememberReduceMotion()
            AnimatedContent(
                targetState = price,
                transitionSpec = {
                    val change = if (reduce) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val dir = if (targetState < initialState) -1 else 1
                        (slideInVertically(Motion.enter(false)) { h -> dir * h } + fadeIn(Motion.enter(false))) togetherWith
                            (slideOutVertically(Motion.exit(false)) { h -> -dir * h } + fadeOut(Motion.exit(false)))
                    }
                    change.using(SizeTransform(clip = true))
                },
                label = "price",
            ) { p ->
                SegmentDigits(String.format(APP_LOCALE, "%.3f", p), digitHeight)
            }
            caption?.let {
                Text(it, color = SegmentLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

/** Título de bloque, anunciado como encabezado por TalkBack. */
@Composable
fun BlockTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.semantics { heading() },
    )
}

/** Barra superior: flecha, icono de acento y título. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkTopBar(
    title: String,
    icon: ImageVector?,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    StatusBarIcons()
    TopAppBar(
        colors = abasteceHojeTopBarColors(),
        navigationIcon = {
            onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = AbasteceHoje.colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
            }
        },
        actions = actions,
    )
}

/** Logotipo de la marca en un cuadrado blanco (los logotipos están pensados para fondo blanco). */
@Composable
fun BrandTile(brand: String, size: Dp = 44.dp) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = if (AbasteceHoje.colors.isDark) null else BorderStroke(1.dp, AbasteceHoje.colors.cardBorder),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) { BrandBadge(brand, size = size - 6.dp) }
    }
}

/** Fila de gasolinera: logotipo, nombre, distancia y lo que vaya a la derecha (precio, estrella…). */
@Composable
fun StationRow(
    station: PricedStation,
    km: Double?,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = "Ver o posto", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandTile(station.brand)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stationTitle(station), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (km != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(" " + formatKm(km), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = trailing)
    }
}

/**
 * Pastilla de filtro, como «Gasolina 95 ▾». El área táctil llega a 48 dp aunque
 * la pastilla se vea más baja.
 */
@Composable
fun FilterPill(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    val c = MaterialTheme.colorScheme
    val bg = if (selected) c.primaryContainer else AbasteceHoje.colors.card
    val fg = if (selected) c.onPrimaryContainer else c.onSurface
    Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .clip(shape)
                .background(bg, shape)
                .border(1.dp, if (selected) c.primary else c.outline, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
                trailing?.invoke()
            }
        }
    }
}

/** Punto de color pequeño. */
@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** Qué tipo de noticia es un aviso: buena (baja), mala (sube), aviso o neutra. */
enum class Tone { GOOD, BAD, WARN, NEUTRAL }

/** Color del tono según el tema. */
@Composable
fun Tone.color(): Color = when (this) {
    Tone.GOOD -> AbasteceHoje.colors.down
    Tone.BAD -> AbasteceHoje.colors.up
    Tone.WARN -> AbasteceHoje.colors.warning
    Tone.NEUTRAL -> AbasteceHoje.colors.accent
}
