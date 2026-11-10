package com.chungaro.combustible.nearby

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Colores de cada sección, para reconocerlas de un vistazo. */
object SectionColors {
    val Teal = Color(0xFF15803D)
    val Blue = Color(0xFF1D4ED8)
    val Amber = Color(0xFFD97706)
    val Purple = Color(0xFF7C3AED)
}

/** Espacio entre secciones (más que dentro de cada una). */
val SectionSpacing = 24.dp

/**
 * Bloque de una sección: tarjeta con sombra, cabecera con icono en un círculo
 * de color, título, subtítulo de una línea, contenido y nota al pie opcional.
 */
@Composable
fun SectionCard(
    icon: String,
    accent: Color,
    title: String,
    subtitle: String? = null,
    footnote: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(icon, accent, title, subtitle)
            content()
            footnote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Cabecera de sección: icono en círculo de color + título en negrita + subtítulo. */
@Composable
fun SectionTitle(icon: String, accent: Color, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = accent.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
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

/** Bloque interior de una sección (fondo suave, sin sombra), para no anidar tarjetas. */
@Composable
fun InnerBlock(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val color = MaterialTheme.colorScheme.surfaceContainer
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth()) { content() }
    }
}
