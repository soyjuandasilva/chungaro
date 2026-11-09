package com.chungaro.combustible.prices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale

private const val MAX_RESULTS = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(vm: PricesViewModel, bottomBar: @Composable () -> Unit) {
    var pickDistrict by rememberSaveable { mutableStateOf(false) }
    val districtName = DgegApi.districts.firstOrNull { it.first == vm.districtId }?.second

    val filtered = remember(vm.stations, vm.query) {
        val q = normalize(vm.query)
        if (q.isBlank()) vm.stations
        else vm.stations.filter {
            normalize("${it.name} ${it.brand} ${it.municipality} ${it.locality} ${it.address}").contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Precios en Portugal") },
                actions = {
                    IconButton(onClick = { vm.refreshFavorites() }, enabled = vm.favorites.isNotEmpty()) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar precios")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Mis gasolineras") }
            if (vm.favoritesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            vm.favoritesError?.let { msg -> item { ErrorText(msg) } }
            if (vm.favorites.isEmpty()) {
                item {
                    Hint("Busca tu gasolinera abajo y márcala con ♥ para ver aquí sus precios cada vez que abras la app.")
                }
            } else {
                items(vm.favorites, key = { "fav-$it" }) { id ->
                    val d = vm.favoriteDetails[id]
                    if (d != null) {
                        FavoriteCard(d, onClick = { vm.openStation(id) })
                    } else if (!vm.favoritesLoading) {
                        Card(Modifier.fillMaxWidth().clickable { vm.openStation(id) }) {
                            Text("Gasolinera $id: sin datos", Modifier.padding(16.dp))
                        }
                    }
                }
            }

            item { SectionTitle("Buscar gasolinera", Modifier.padding(top = 12.dp)) }
            item {
                OutlinedButton(onClick = { pickDistrict = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Text("  " + (districtName ?: "Elegir distrito"))
                }
            }
            if (vm.districtId != null) {
                item {
                    OutlinedTextField(
                        value = vm.query,
                        onValueChange = { vm.query = it },
                        label = { Text("Nombre, marca o localidad") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            when {
                vm.stationsLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                vm.stationsError != null -> item {
                    Column {
                        ErrorText(vm.stationsError!!)
                        TextButton(onClick = { vm.retryStations() }) { Text("Reintentar") }
                    }
                }
                vm.districtId != null -> {
                    item {
                        Text(
                            if (filtered.size > MAX_RESULTS) "${filtered.size} gasolineras, mostrando $MAX_RESULTS. Escribe para filtrar."
                            else "${filtered.size} gasolineras",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(filtered.take(MAX_RESULTS), key = { "st-${it.id}" }) { s ->
                        StationRow(s, isFavorite = s.id in vm.favorites, onClick = { vm.openStation(s.id) })
                    }
                }
            }
            item {
                Text(
                    "Fuente: DGEG — precoscombustiveis.dgeg.gov.pt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (pickDistrict) {
        AlertDialog(
            onDismissRequest = { pickDistrict = false },
            title = { Text("Distrito") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(DgegApi.districts, key = { it.first }) { (id, name) ->
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (id == vm.districtId) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.selectDistrict(id); pickDistrict = false }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickDistrict = false }) { Text("Cerrar") } },
        )
    }

    if (vm.selectedId != null) {
        StationDialog(vm)
    }
}

@Composable
private fun StationDialog(vm: PricesViewModel) {
    val d = vm.selected
    AlertDialog(
        onDismissRequest = { vm.closeStation() },
        title = { Text(d?.name?.ifBlank { null } ?: "Gasolinera") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    d != null -> {
                        if (d.brand.isNotBlank()) Text(d.brand, fontWeight = FontWeight.SemiBold)
                        if (d.address.isNotBlank()) Text(d.address, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        if (d.fuels.isEmpty()) Text("Esta gasolinera no informa precios.")
                        d.fuels.forEach { FuelLine(it) }
                    }
                    vm.selectedError != null -> ErrorText(vm.selectedError!!)
                    else -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            if (d != null) {
                val fav = d.id in vm.favorites
                TextButton(onClick = { vm.toggleFavorite(d) }) {
                    Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null)
                    Text(if (fav) "  Quitar de favoritas" else "  Agregar a favoritas")
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.closeStation() }) { Text("Cerrar") } },
    )
}

@Composable
private fun FavoriteCard(d: StationDetail, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                listOf(d.brand, d.name).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            if (d.address.isNotBlank()) {
                Text(d.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            d.fuels.forEach { FuelLine(it) }
        }
    }
}

@Composable
private fun FuelLine(f: FuelPrice) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(f.fuel, style = MaterialTheme.typography.bodyMedium)
            if (f.updated.isNotBlank()) {
                Text("Actualizado: ${f.updated}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            f.price?.let { String.format(Locale.getDefault(), "%.3f €/L", it) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StationRow(s: Station, isFavorite: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.name.ifBlank { "Gasolinera ${s.id}" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOf(s.brand, s.locality.ifBlank { s.municipality }).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isFavorite) Icon(Icons.Filled.Favorite, contentDescription = "Favorita", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .trim()
