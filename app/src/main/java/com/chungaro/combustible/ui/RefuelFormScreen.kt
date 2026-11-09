package com.chungaro.combustible.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Refuel
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/** Precio de hoy de una gasolinera favorita, para rellenar el formulario. */
data class PriceSuggestion(val label: String, val price: Double)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RefuelFormScreen(
    existing: Refuel?,
    lastOdometer: Double?,
    settings: AppSettings,
    priceSuggestions: List<PriceSuggestion> = emptyList(),
    onSave: (Refuel) -> Unit,
    onDelete: (Refuel) -> Unit,
    onBack: () -> Unit,
) {
    val u = settings.unitLabel
    var epochDay by rememberSaveable { mutableLongStateOf(existing?.epochDay ?: LocalDate.now().toEpochDay()) }
    var odometer by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.odometer, 1) } ?: "") }
    var quantity by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.quantity) } ?: "") }
    var price by rememberSaveable {
        mutableStateOf(existing?.takeIf { it.quantity > 0 }?.let { editableNumber(it.totalCost / it.quantity) } ?: "")
    }
    var total by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.totalCost, 2) } ?: "") }
    var fullTank by rememberSaveable { mutableStateOf(existing?.fullTank ?: true) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val odo = parseNumber(odometer)
    val qty = parseNumber(quantity)
    val tot = parseNumber(total)
    val valid = odo != null && odo >= 0 && qty != null && qty > 0 && tot != null && tot >= 0

    // Al escribir cantidad o precio se recalcula el total; al escribir el total, el precio.
    fun recalcTotal() {
        val q = parseNumber(quantity)
        val p = parseNumber(price)
        if (q != null && p != null) total = editableNumber(q * p, 2)
    }
    fun recalcPrice() {
        val q = parseNumber(quantity)
        val t = parseNumber(total)
        if (q != null && q > 0 && t != null) price = editableNumber(t / q)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Nuevo abastecimiento" else "Editar abastecimiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Text("  " + formatDate(epochDay))
            }

            NumberField(
                value = odometer,
                onValueChange = { odometer = it },
                label = "Kilometraje (odómetro)",
                suffix = "km",
                supporting = lastOdometer?.let { "Último registro: ${formatNumber(it, 0)} km" },
                isError = odo != null && lastOdometer != null && odo < lastOdometer,
            )
            NumberField(
                value = quantity,
                onValueChange = { quantity = it; recalcTotal() },
                label = "Cantidad cargada",
                suffix = u,
            )
            NumberField(
                value = price,
                onValueChange = { price = it; recalcTotal() },
                label = "Precio por ${if (u == "gal") "galón" else "litro"}",
                prefix = settings.currency,
            )
            if (priceSuggestions.isNotEmpty()) {
                Text("Precios de hoy (toca para usar)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priceSuggestions.forEach { sug ->
                        AssistChip(
                            onClick = { price = editableNumber(sug.price); recalcTotal() },
                            label = { Text("${sug.label}: ${formatNumber(sug.price, 3)}") },
                        )
                    }
                }
            }
            NumberField(
                value = total,
                onValueChange = { total = it; recalcPrice() },
                label = "Total pagado",
                prefix = settings.currency,
                imeAction = ImeAction.Done,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tanque lleno", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Llena el tanque para que la app calcule el rendimiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = fullTank, onCheckedChange = { fullTank = it })
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Estación, tipo de combustible…") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onSave(
                        Refuel(
                            id = existing?.id ?: 0,
                            epochDay = epochDay,
                            odometer = odo!!,
                            quantity = qty!!,
                            totalCost = tot!!,
                            fullTank = fullTank,
                            notes = notes.trim(),
                        ),
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { epochDay = Math.floorDiv(it, MILLIS_PER_DAY) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este abastecimiento?") },
            text = { Text("No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(existing) }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    prefix: String? = null,
    suffix: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = (if (isError) "Es menor que el último kilometraje registrado" else supporting)
            ?.let { msg -> { Text(msg) } },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}
