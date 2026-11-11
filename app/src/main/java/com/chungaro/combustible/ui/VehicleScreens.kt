package com.chungaro.combustible.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.VEHICLE_TYPES
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.VehicleCatalog
import com.chungaro.combustible.data.description
import com.chungaro.combustible.data.emoji
import com.chungaro.combustible.data.vehicleType
import com.chungaro.combustible.nearby.DEFAULT_FUEL_ID
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.FuelPill
import java.time.Year

/** Lista de vehículos: elegir el que está en uso, añadir y editar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    vehicles: List<Vehicle>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            StatusBarIcons()
            TopAppBar(
                colors = atestaTopBarColors(),
                title = { Text("Os meus veículos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Adicionar veículo") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Toca num veículo para o usares no Início e nos Abastecimentos, ou em «Editar» para mudares os dados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(vehicles, key = { it.id }) { v ->
                val selected = v.id == selectedId
                Card(
                    Modifier.fillMaxWidth().clickable { onSelect(v.id) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(v.emoji, style = MaterialTheme.typography.displaySmall)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(v.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val desc = v.description
                            if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodyMedium)
                            FUEL_TYPES.firstOrNull { it.id == v.fuelTypeId }?.let { FuelPill(it) }
                            if (selected) {
                                Text("Em uso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { onEdit(v.id) }) { Text("Editar") }
                    }
                }
            }
        }
    }
}

/** Alta o edición de un vehículo. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleEditScreen(
    existing: Vehicle?,
    canDelete: Boolean,
    onSave: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
    onBack: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(existing?.type ?: "car") }
    var brand by rememberSaveable { mutableStateOf(existing?.brand ?: "") }
    var model by rememberSaveable { mutableStateOf(existing?.model ?: "") }
    var year by rememberSaveable { mutableStateOf(existing?.year?.toString() ?: "") }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var fuel by rememberSaveable { mutableIntStateOf(existing?.fuelTypeId ?: DEFAULT_FUEL_ID) }
    var tank by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.tankLiters, 1) } ?: "50") }
    var cons by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.consumption, 1) } ?: "6.5") }
    var consEdited by rememberSaveable { mutableStateOf(existing != null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val thisYear = Year.now().value
    val yearV = year.toIntOrNull()
    val yearOk = year.isBlank() || (yearV != null && yearV in 1900..thisYear + 1)
    val tankV = parseNumber(tank)
    val consV = parseNumber(cons)
    val autoName = listOf(brand.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ")
    val finalName = name.trim().ifBlank { autoName }.ifBlank { vehicleType(type).label }
    val valid = yearOk && tankV != null && tankV > 0 && consV != null && consV > 0

    val brandSuggestions = VehicleCatalog.brands(type)
        .filter { brand.isBlank() || it.contains(brand.trim(), ignoreCase = true) }
        .filterNot { it.equals(brand.trim(), ignoreCase = true) }
        .take(12)
    val modelSuggestions = VehicleCatalog.models(type, brand)
        .filter { model.isBlank() || it.contains(model.trim(), ignoreCase = true) }
        .filterNot { it.equals(model.trim(), ignoreCase = true) }

    Scaffold(
        topBar = {
            StatusBarIcons()
            TopAppBar(
                colors = atestaTopBarColors(),
                title = { Text(if (existing == null) "Novo veículo" else "Editar veículo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    if (existing != null && canDelete) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Tipo de veículo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VEHICLE_TYPES.forEach { t ->
                    FilterChip(
                        selected = type == t.id,
                        onClick = {
                            type = t.id
                            if (!consEdited) cons = editableNumber(t.typicalConsumption, 1)
                        },
                        label = { Text("${t.emoji} ${t.label}") },
                    )
                }
            }

            SectionTitle("Marca")
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marca") },
                placeholder = { Text("Renault, Honda…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (brandSuggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    brandSuggestions.forEach { b -> SuggestionChip(onClick = { brand = b; model = "" }, label = { Text(b) }) }
                }
            }

            SectionTitle("Modelo")
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modelo") },
                placeholder = { Text("Clio, PCX 125…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (modelSuggestions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    modelSuggestions.forEach { m -> SuggestionChip(onClick = { model = m }, label = { Text(m) }) }
                }
            }

            OutlinedTextField(
                value = year,
                onValueChange = { v -> year = v.filter { it.isDigit() }.take(4) },
                label = { Text("Ano") },
                placeholder = { Text("$thisYear") },
                isError = !yearOk,
                supportingText = (if (!yearOk) "Ano entre 1900 e ${thisYear + 1}" else null)?.let { msg -> { Text(msg) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome ou alcunha (opcional)") },
                placeholder = { Text(autoName.ifBlank { "O meu carro" }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Combustível")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FUEL_TYPES.forEach { f ->
                    FilterChip(selected = fuel == f.id, onClick = { fuel = f.id }, label = { Text(f.short) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tank,
                    onValueChange = { v -> tank = v.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Ao encher") },
                    suffix = { Text("L") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = cons,
                    onValueChange = { v -> cons = v.filter { it.isDigit() || it == '.' || it == ',' }; consEdited = true },
                    label = { Text("Consumo") },
                    suffix = { Text("L/100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Os litros ao encher e o consumo servem para calcular se compensa ir à bomba mais barata. " +
                    "Quando tiveres histórico de abastecimentos, usamos o teu consumo real.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        Vehicle(
                            id = existing?.id ?: 0,
                            name = finalName,
                            fuelTypeId = fuel,
                            tankLiters = tankV!!,
                            consumption = consV!!,
                            type = type,
                            brand = brand.trim(),
                            model = model.trim(),
                            year = yearV,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar «$finalName»") }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar «${existing.name}»?") },
            text = { Text("Os abastecimentos e lembretes deste veículo também serão apagados. Esta ação não pode ser anulada.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(existing) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
