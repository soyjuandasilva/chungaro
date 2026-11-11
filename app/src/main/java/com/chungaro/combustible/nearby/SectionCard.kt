package com.chungaro.combustible.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.ui.Atesta

/** Colores de acento de las secciones, según el tema. */
object SectionColors {
    val Teal: Color @Composable @ReadOnlyComposable get() = Atesta.colors.accent
    val Blue: Color @Composable @ReadOnlyComposable get() = Atesta.colors.info
    val Amber: Color @Composable @ReadOnlyComposable get() = Atesta.colors.warning
    val Purple: Color @Composable @ReadOnlyComposable get() = Atesta.colors.purple
}

/** Espacio entre secciones (más que dentro de cada una). */
val SectionSpacing = 24.dp

/**
 * Bloque de una sección: tarjeta, cabecera con icono, título, subtítulo de una
 * línea, contenido y nota al pie opcional.
 */
@Composable
fun SectionCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String? = null,
    footnote: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().cardSurface().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(icon, accent, title, subtitle)
        content()
        footnote?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cabecera de sección: icono en un círculo suave del acento + título + subtítulo. */
@Composable
fun SectionTitle(icon: ImageVector, accent: Color, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).background(accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Bloque interior de una sección (fondo suave, sin borde ni sombra), para no anidar tarjetas. */
@Composable
fun InnerBlock(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
    }
}
