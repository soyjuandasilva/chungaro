package com.chungaro.combustible.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.ui.AtestaBorder
import com.chungaro.combustible.ui.AtestaCard
import com.chungaro.combustible.ui.AtestaGreenLight
import com.chungaro.combustible.ui.StatusBarIcons
import com.chungaro.combustible.ui.atestaTopBarColors
import java.util.Locale

/** Precio con coma decimal y €: "1,659€". */
fun euro3(v: Double): String = String.format(Locale.getDefault(), "%.3f€", v)

/** Relleno de las tarjetas: un poco más claro arriba, como el diseño. */
val CardBrush: Brush = Brush.verticalGradient(listOf(Color(0xFF15402C), Color(0xFF0C2619)))
/** Borde con brillo arriba que se apaga hacia abajo. */
val CardBorderBrush: Brush = Brush.verticalGradient(listOf(Color(0xFF3F7A5C), Color(0xFF173828)))
/** Verde de los botones y etiquetas de precio, con volumen. */
val GreenBrush: Brush = Brush.verticalGradient(listOf(Color(0xFF34D46F), Color(0xFF15803D)))

/** Aplica el aspecto de tarjeta: sombra, degradado y borde brillante. */
fun Modifier.glassCard(shape: Shape = RoundedCornerShape(18.dp), elevation: Dp = 10.dp): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color.Black)
    .background(CardBrush, shape)
    .border(BorderStroke(1.dp, CardBorderBrush), shape)
    .clip(shape)

/** Tarjeta oscura con profundidad, base de todo el diseño. */
@Composable
fun DarkCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    @Suppress("UNUSED_PARAMETER") color: Color = AtestaCard,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().glassCard(shape), content = content)
}

/** Etiqueta de precio con fondo de color; texto oscuro si el fondo es claro. */
@Composable
fun PriceChip(price: Double, color: Color = AtestaGreenLight, caption: String? = null, big: Boolean = false) {
    val ink = if (color.luminance() > 0.45f) Color(0xFF052E16) else Color.White
    val shape = RoundedCornerShape(10.dp)
    // Degradado del propio color (más claro arriba) y un brillo en el borde
    val light = Color(
        (color.red + (1 - color.red) * 0.25f),
        (color.green + (1 - color.green) * 0.25f),
        (color.blue + (1 - color.blue) * 0.25f),
    )
    val dark = Color(color.red * 0.78f, color.green * 0.78f, color.blue * 0.78f)
    Box(
        Modifier
            .shadow(4.dp, shape, spotColor = color, ambientColor = Color.Black)
            .background(Brush.verticalGradient(listOf(light, color, dark)), shape)
            .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent))), shape),
    ) {
        Column(
            Modifier.padding(horizontal = if (big) 12.dp else 10.dp, vertical = if (caption != null) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                euro3(price),
                color = ink,
                fontWeight = FontWeight.ExtraBold,
                style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false,
            )
            caption?.let { Text(it, color = ink.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
    }
}

/** Barra superior: flecha, icono verde y título, sobre el fondo oscuro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkTopBar(
    title: String,
    icon: ImageVector?,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    StatusBarIcons(darkBackground = true)
    TopAppBar(
        colors = atestaTopBarColors(),
        navigationIcon = {
            onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = AtestaGreenLight, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, fontWeight = FontWeight.ExtraBold)
            }
        },
        actions = actions,
    )
}

/** Logotipo de la marca en un cuadrado blanco redondeado, como en las listas. */
@Composable
fun BrandTile(brand: String, size: Dp = 44.dp) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White, modifier = Modifier.size(size)) {
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
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
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

/** Pastilla de filtro con flecha, como «Gasolina 95 ▾». */
@Composable
fun FilterPill(text: String, selected: Boolean = false, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .shadow(6.dp, shape, spotColor = if (selected) AtestaGreenLight else Color.Black)
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF1E5A3B), Color(0xFF0F3322))) else CardBrush, shape)
            .border(BorderStroke(1.dp, if (selected) SolidColor(AtestaGreenLight) else CardBorderBrush), shape)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = if (selected) AtestaGreenLight else Color.White)
            trailing?.invoke()
        }
    }
}

/** Punto de color pequeño. */
@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(size)) {}
}
