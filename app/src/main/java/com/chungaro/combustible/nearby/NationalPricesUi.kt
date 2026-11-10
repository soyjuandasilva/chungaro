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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chungaro.combustible.ui.APP_LOCALE
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Up = Color(0xFFDC2626)
private val Down = Color(0xFF16A34A)
private val mondayFormat = DateTimeFormatter.ofPattern("d/MM", APP_LOCALE)

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
 * "Preço dos combustíveis hoje e na próxima semana": precio medio actual de
 * todas las bombas del país y estimación para la semana que viene.
 */
@Composable
fun NationalPricesCard(vm: NearbyViewModel, myFuelId: Int) {
    val monday = remember { nextMonday().format(mondayFormat) }
    Card(Modifier.fillMaxWidth()) {
        Column {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp)) {
                Text(
                    "Preço dos combustíveis hoje e na próxima semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Preço médio em Portugal, calculado com todas as bombas que publicam preço na DGEG. " +
                        "É uma média: em cada bomba o preço é diferente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeadCell("Combustível", Modifier.weight(1.3f), TextAlign.Start)
                HeadCell("Preço atual\n(média)", Modifier.weight(1f), TextAlign.End)
                HeadCell("Próxima semana\n(a partir de $monday)", Modifier.weight(1.3f), TextAlign.End)
            }
            // Tu combustible primero.
            val ordered = FUEL_TYPES.sortedBy { if (it.id == myFuelId) 0 else 1 }
            ordered.forEachIndexed { i, fuel ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                NationalRow(vm, fuel, highlight = fuel.id == myFuelId)
            }
            Text(
                "A previsão é uma estimativa feita pela app com a evolução dos preços dos últimos dias. " +
                    "Em Portugal os preços costumam mudar à segunda-feira.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@Composable
private fun HeadCell(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
private fun NationalRow(vm: NearbyViewModel, fuel: FuelType, highlight: Boolean) {
    // Leer vm.rows hace que la fila se actualice al llegar precios nuevos.
    val state = vm.rows[fuel.id]
    val national = remember(state) { vm.nationalPrice(fuel.id) }
    val nationalHistory by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.NATIONAL_AVG) }.collectAsStateWithLifecycle(emptyList())
    val zoneHistory by remember(fuel.id) { vm.history(fuel.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
    // Con poco histórico nacional, se usa la tendencia de tu zona aplicada a la media del país.
    val forecast = remember(national, nationalHistory, zoneHistory) {
        national?.let { forecastNextWeek(nationalHistory, it.average) ?: forecastNextWeek(zoneHistory, it.average) }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) fuel.uiColor.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.3f)) {
            FuelPill(fuel, compact = true)
            if (highlight) {
                Text("O teu combustível", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (national == null) {
            Box(Modifier.weight(2.3f), contentAlignment = Alignment.CenterEnd) {
                if (state is FuelRowState.Error) {
                    Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            return@Row
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(price3(national.average), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Text("€/L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            if (forecast == null) {
                Text("A calcular…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                Spacer(Modifier.height(2.dp))
                Text("precisa de 1 semana de dados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
            } else {
                val pct = forecast.changePct
                val up = pct > 0.2
                val down = pct < -0.2
                Text("≈ " + price3(forecast.price), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(
                    when {
                        up -> "↗ sobe ~" + pct1(pct)
                        down -> "↘ desce ~" + pct1(-pct)
                        else -> "→ estável"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        up -> Up
                        down -> Down
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

private fun price3(v: Double) = String.format(Locale.getDefault(), "%.3f", v)
private fun pct1(v: Double) = String.format(Locale.getDefault(), "%.1f%%", v)
