package com.chungaro.combustible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Refuel
import com.chungaro.combustible.data.RefuelRow
import com.chungaro.combustible.data.Stats
import com.chungaro.combustible.data.Summary
import com.chungaro.combustible.data.pricePerUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    stats: Stats,
    loading: Boolean,
    settings: AppSettings,
    onAdd: () -> Unit,
    onOpen: (Refuel) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chungaro") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Abastecer") },
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(stats.summary, settings) }
            if (stats.rows.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay abastecimientos.\nToca «Abastecer» para registrar el primero.",
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        "Historial",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(stats.rows.asReversed(), key = { it.refuel.id }) { row ->
                    RefuelCard(row, settings, onClick = { onOpen(row.refuel) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(s: Summary, settings: AppSettings) {
    val u = settings.unitLabel
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Gastado este mes", style = MaterialTheme.typography.labelLarge)
            Text(
                settings.money(s.monthCost),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatNumber(s.monthQuantity)} $u cargados",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Rendimiento", s.avgEfficiency?.let { "${formatNumber(it)} km/$u" } ?: "—", Modifier.weight(1f))
                Stat("Costo por km", s.avgCostPerKm?.let { settings.money(it) } ?: "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Gasto total", settings.money(s.totalCost), Modifier.weight(1f))
                Stat("Combustible total", "${formatNumber(s.totalQuantity)} $u", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Precio promedio", s.avgPrice?.let { "${settings.money(it)}/$u" } ?: "—", Modifier.weight(1f))
                Stat("Km registrados", formatNumber(s.totalDistance, 0), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RefuelCard(row: RefuelRow, settings: AppSettings, onClick: () -> Unit) {
    val r = row.refuel
    val u = settings.unitLabel
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(r.epochDay),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    settings.money(r.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "${formatNumber(r.quantity)} $u · ${settings.money(r.pricePerUnit())}/$u · ${formatNumber(r.odometer, 0)} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val details = buildList {
                row.distance?.let { add("+${formatNumber(it, 0)} km") }
                row.efficiency?.let { add("${formatNumber(it)} km/$u") }
                row.costPerKm?.let { add("${settings.money(it)}/km") }
            }
            if (details.isNotEmpty()) {
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
            if (!r.fullTank || r.notes.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!r.fullTank) SuggestionChip(onClick = onClick, label = { Text("Carga parcial") })
                    if (r.notes.isNotBlank()) {
                        Text(r.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}
