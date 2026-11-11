package com.chungaro.combustible.prices

import androidx.compose.foundation.clickable
import com.chungaro.combustible.ui.StatusBarIcons
import com.chungaro.combustible.ui.atestaTopBarColors
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import java.util.Locale
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.FuelType
import com.chungaro.combustible.nearby.uiColor
import com.chungaro.combustible.nearby.BrandBadge

private const val MAX_RESULTS = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricesScreen(vm: PricesViewModel, onBack: () -> Unit) {
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
            StatusBarIcons()
            TopAppBar(
                colors = atestaTopBarColors(),
                title = { Text("Postos por distrito", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshFavorites() }, enabled = vm.favorites.isNotEmpty()) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Atualizar preços")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Os meus postos") }
            if (vm.favoritesLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            vm.favoritesError?.let { msg -> item { ErrorText(msg) } }
            if (vm.favorites.isEmpty()) {
                item {
                    Hint("Procura o teu posto aqui em baixo e marca-o com a estrela para veres os preços sempre que abrires a app.")
                }
            } else {
                items(vm.favorites, key = { "fav-$it" }) { id ->
                    val d = vm.favoriteDetails[id]
                    if (d != null) {
                        FavoriteCard(d, onClick = { vm.openStation(id) })
                    } else if (!vm.favoritesLoading) {
                        Card(Modifier.fillMaxWidth().clickable { vm.openStation(id) }) {
                            Text("Posto $id: sem dados", Modifier.padding(16.dp))
                        }
                    }
                }
            }

            item { SectionTitle("Procurar posto", Modifier.padding(top = 12.dp)) }
            item {
                OutlinedButton(onClick = { pickDistrict = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Text("  " + (districtName ?: "Escolher distrito"))
                }
            }
            if (vm.districtId != null) {
                item {
                    OutlinedTextField(
                        value = vm.query,
                        onValueChange = { vm.query = it },
                        label = { Text("Nome, marca ou localidade") },
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
                        TextButton(onClick = { vm.retryStations() }) { Text("Tentar de novo") }
                    }
                }
                vm.districtId != null -> {
                    item {
                        Text(
                            if (filtered.size > MAX_RESULTS) "${filtered.size} postos; mostramos $MAX_RESULTS. Escreve para filtrar."
                            else "${filtered.size} postos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(filtered.take(MAX_RESULTS)) { s ->
                        StationRow(s, isFavorite = s.id in vm.favorites, onClick = { vm.openStation(s.id) })
                    }
                }
            }
            item {
                Text(
                    "Fonte: DGEG — precoscombustiveis.dgeg.gov.pt",
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
            confirmButton = { TextButton(onClick = { pickDistrict = false }) { Text("Fechar") } },
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
        title = { Text(d?.name?.ifBlank { null } ?: "Posto") },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    d != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandBadge(d.brand, size = 32.dp, modifier = Modifier.padding(end = 8.dp))
                            Text(d.brand.ifBlank { "Sem marca" }, fontWeight = FontWeight.SemiBold)
                        }
                        if (d.address.isNotBlank()) Text(d.address, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        FuelGrid(d.fuels)
                        lastUpdate(d.fuels)?.let {
                            Text("Atualizado: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                    Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = null)
                    Text(if (fav) "  Remover dos favoritos" else "  Adicionar aos favoritos")
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.closeStation() }) { Text("Fechar") } },
    )
}

@Composable
private fun FavoriteCard(d: StationDetail, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(d.brand, size = 40.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(d.name.ifBlank { d.brand }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    shortAddress(d.address)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            FuelGrid(d.fuels)
            lastUpdate(d.fuels)?.let {
                Text("Atualizado: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Precios en cuadritos de colores, 3 por fila. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuelGrid(fuels: List<FuelPrice>) {
    if (fuels.isEmpty()) {
        Text("Este posto não indica preços.", style = MaterialTheme.typography.bodySmall)
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 3,
        modifier = Modifier.fillMaxWidth(),
    ) {
        fuels.forEach { f ->
            val type = fuelTypeFor(f.fuel)
            val color = type?.uiColor ?: MaterialTheme.colorScheme.outline
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.10f),
                modifier = Modifier.weight(1f),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text(
                        type?.short ?: f.fuel,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        f.price?.let { String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f", it) } ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

/** Relaciona el nombre de la DGEG ("Gasóleo simples") con nuestros tipos. */
private fun fuelTypeFor(name: String): FuelType? {
    val n = normalize(name)
    return FUEL_TYPES.firstOrNull { normalize(it.name) == n }
        ?: FUEL_TYPES.firstOrNull { n.startsWith(normalize(it.name)) || normalize(it.name).startsWith(n) }
}

/** Fecha de actualización más reciente, en formato corto "09/11 20:30". */
private fun lastUpdate(fuels: List<FuelPrice>): String? {
    val raw = fuels.map { it.updated }.filter { it.isNotBlank() }.maxOrNull() ?: return null
    val m = Regex("""(\d{4})-(\d{2})-(\d{2})\s+(\d{2}:\d{2})""").find(raw) ?: return raw
    val (_, mo, d, t) = m.destructured
    return "$d/$mo $t"
}

/** Solo calle y localidad (sin el código postal). */
private fun shortAddress(address: String): String? =
    address.split(",").map { it.trim() }.filter { it.isNotBlank() && !Regex("""\d{4}-\d{3}""").matches(it) }
        .take(2).joinToString(", ").ifBlank { null }

@Composable
private fun FuelLine(f: FuelPrice) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(f.fuel, style = MaterialTheme.typography.bodyMedium)
            if (f.updated.isNotBlank()) {
                Text("Atualizado: ${f.updated}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            f.price?.let { String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f €/L", it) } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StationRow(s: Station, isFavorite: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandBadge(s.brand, size = 36.dp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name.ifBlank { "Posto ${s.id}" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOf(s.brand, s.locality.ifBlank { s.municipality }).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isFavorite) Icon(Icons.Filled.Star, contentDescription = "Favorita", tint = com.chungaro.combustible.ui.Atesta.colors.star)
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

private fun normalize(s: String): String = DgegApi.normalize(s)
