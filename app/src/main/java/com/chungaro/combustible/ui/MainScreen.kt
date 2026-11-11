package com.chungaro.combustible.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Refuel
import com.chungaro.combustible.data.RefuelRow
import com.chungaro.combustible.data.Reminder
import com.chungaro.combustible.data.Stats
import com.chungaro.combustible.data.Summary
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.description
import com.chungaro.combustible.data.emoji
import com.chungaro.combustible.data.pricePerUnit

private const val SECTION_PRICES = 0
private const val SECTION_HISTORY = 1
private const val SECTION_CHARTS = 2
private const val SECTION_MAINTENANCE = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    stats: Stats,
    loading: Boolean,
    settings: AppSettings,
    vehicles: List<Vehicle>,
    vehicle: Vehicle?,
    reminders: List<Reminder>,
    onAdd: () -> Unit,
    onOpen: (Refuel) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectVehicle: (Long) -> Unit,
    onEditVehicle: (Vehicle?) -> Unit,
    onManageVehicles: () -> Unit,
    onSaveReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onBack: () -> Unit = {},
    /** Precios para el vehículo (consejos, mejor opción y otros combustibles). */
    pricesContent: @Composable () -> Unit = {},
) {
    var section by rememberSaveable { mutableIntStateOf(SECTION_PRICES) }
    var menu by rememberSaveable { mutableStateOf(false) }
    var editingReminder by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            StatusBarIcons()
            TopAppBar(
                colors = atestaTopBarColors(),
                title = {
                    Box {
                        Row(
                            Modifier.clickable { menu = true }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(vehicle?.let { "${it.emoji} ${it.name}" } ?: "Meu veículo", fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Mudar de veículo")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${v.emoji} ${v.name}", fontWeight = if (v.id == vehicle?.id) FontWeight.Bold else FontWeight.Normal)
                                            if (v.description.isNotBlank()) Text(v.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = { menu = false; onSelectVehicle(v.id) },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Editar este veículo") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = { menu = false; onEditVehicle(vehicle) },
                            )
                            DropdownMenuItem(
                                text = { Text("Adicionar veículo") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                onClick = { menu = false; onEditVehicle(null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Os meus veículos") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                onClick = { menu = false; onManageVehicles() },
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        floatingActionButton = {
            when (section) {
                SECTION_HISTORY -> ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Abastecer") },
                )
                SECTION_MAINTENANCE -> ExtendedFloatingActionButton(
                    onClick = { editingReminder = 0L },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Lembrete") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SectionTabs(listOf("Preços", "Histórico", "Gráficos", "Manutenção"), section) { section = it }
            if (section == SECTION_PRICES) {
                pricesContent()
                return@Column
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            when (section) {
                SECTION_HISTORY -> HistoryList(stats, settings, onOpen)
                SECTION_CHARTS -> ChartsSection(stats, settings)
                else -> MaintenanceSection(
                    reminders = reminders,
                    lastOdometer = stats.rows.lastOrNull()?.refuel?.odometer,
                    onEdit = { editingReminder = it.id },
                    onToggleDone = { onSaveReminder(it.copy(done = !it.done)) },
                )
            }
        }
    }

    editingReminder?.let { rid ->
        ReminderDialog(
            existing = reminders.firstOrNull { it.id == rid },
            vehicleId = vehicle?.id ?: 1L,
            onDismiss = { editingReminder = null },
            onSave = { onSaveReminder(it); editingReminder = null },
            onDelete = { onDeleteReminder(it); editingReminder = null },
        )
    }
}

@Composable
private fun HistoryList(stats: Stats, settings: AppSettings, onOpen: (Refuel) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(stats.summary, settings) }
        if (stats.rows.isEmpty()) {
            item {
                Text(
                    "Ainda não há abastecimentos.\nToca em «Abastecer» para registares o primeiro.",
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(stats.rows.asReversed(), key = { it.refuel.id }) { row ->
                RefuelCard(row, settings, onClick = { onOpen(row.refuel) })
            }
        }
    }
}

@Composable
private fun SummaryCard(s: Summary, settings: AppSettings) {
    val u = settings.unitLabel
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Gasto este mês", style = MaterialTheme.typography.labelLarge)
            Text(
                settings.money(s.monthCost),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text("${formatNumber(s.monthQuantity)} $u abastecidos", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Consumo médio", s.avgEfficiency?.let { settings.consumption(it) } ?: "—", Modifier.weight(1f))
                Stat("Custo por km", s.avgCostPerKm?.let { settings.money(it) } ?: "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Gasto total", settings.money(s.totalCost), Modifier.weight(1f))
                Stat("Combustível total", "${formatNumber(s.totalQuantity)} $u", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Preço médio", s.avgPrice?.let { "${settings.moneyPrecise(it)}/$u" } ?: "—", Modifier.weight(1f))
                Stat("Km registados", formatNumber(s.totalDistance, 0), Modifier.weight(1f))
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
                Text(formatDate(r.epochDay), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    settings.money(r.totalCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                "${formatNumber(r.quantity)} $u · ${settings.moneyPrecise(r.pricePerUnit())}/$u · ${formatNumber(r.odometer, 0)} km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val details = buildList {
                row.distance?.let { add("+${formatNumber(it, 0)} km") }
                row.efficiency?.let { add(settings.consumption(it)) }
                row.costPerKm?.let { add("${settings.money(it)}/km") }
            }
            if (details.isNotEmpty()) {
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
            if (!r.fullTank || r.notes.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!r.fullTank) SuggestionChip(onClick = onClick, label = { Text("Abastecimento parcial") })
                    if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

/** Pestañas compactas: las cuatro caben a lo ancho, sin deslizar. */
@Composable
private fun SectionTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    // Continúa la barra superior verde oscuro.
    Column(Modifier.background(AtestaGreen)) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val sel = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(i) }
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .height(3.dp)
                            .background(if (sel) AtestaGreenLight else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
    }
}
