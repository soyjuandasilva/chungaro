package com.chungaro.combustible.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Todos los combustibles a la vista (2 filas de 3), sin deslizar.
 * [hint] va encima, para explicar para qué sirve elegir.
 */
@Composable
fun FuelSelector(
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        hint?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        FUEL_TYPES.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { f ->
                    val sel = f.id == selectedId
                    Surface(
                        onClick = { onSelect(f.id) },
                        shape = RoundedCornerShape(50),
                        color = if (sel) f.uiColor else f.uiColor.copy(alpha = 0.10f),
                        contentColor = if (sel) Color.White else f.uiColor,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            f.short,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        )
                    }
                }
                // Si la última fila quedara incompleta, mantiene el mismo ancho por botón.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Precio medio de hoy de cada combustible en todo Portugal, en grande.
 * Media de todas las bombas con precio publicado en la DGEG.
 */
@Composable
fun NationalPricesCard(vm: NearbyViewModel, myFuelId: Int) {
    // Leer pricesVersion recalcula las medias cuando llegan precios nuevos.
    val version = vm.pricesVersion
    val prices = remember(version) { FUEL_TYPES.associate { it.id to vm.nationalPrice(it.id) } }
    val loading = prices.values.all { it == null }
    SectionCard(
        icon = "💶",
        accent = SectionColors.Teal,
        title = "Preço médio hoje",
        subtitle = "Todas as bombas de Portugal, em €/L",
        footnote = "É uma média: em cada bomba o preço é diferente. Fonte: DGEG.",
    ) {
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
            return@SectionCard
        }
        // Tu combustible primero.
        val ordered = FUEL_TYPES.sortedBy { if (it.id == myFuelId) 0 else 1 }
        ordered.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { fuel ->
                    PriceTile(fuel, prices[fuel.id], mine = fuel.id == myFuelId, modifier = Modifier.weight(1f))
                }
                repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PriceTile(fuel: FuelType, price: NationalPrice?, mine: Boolean, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fuel.uiColor.copy(alpha = if (mine) 0.16f else 0.08f),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                fuel.short + if (mine) " · o teu" else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = fuel.uiColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    price?.let { String.format(Locale.getDefault(), "%.3f", it.average) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    " €/L",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                price?.let { "média de ${it.stations} bombas" } ?: "sem dados",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
