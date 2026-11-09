package com.chungaro.combustible.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.nearby.DEFAULT_FUEL_ID
import com.chungaro.combustible.nearby.FUEL_TYPES

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleDialog(
    existing: Vehicle?,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (Vehicle) -> Unit,
    onDelete: (Vehicle) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var fuel by rememberSaveable { mutableIntStateOf(existing?.fuelTypeId ?: DEFAULT_FUEL_ID) }
    var tank by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.tankLiters, 1) } ?: "50") }
    var cons by rememberSaveable { mutableStateOf(existing?.let { editableNumber(it.consumption, 1) } ?: "6.5") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val tankV = parseNumber(tank)
    val consV = parseNumber(cons)
    val valid = name.isNotBlank() && tankV != null && tankV > 0 && consV != null && consV > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nuevo vehículo" else "Editar vehículo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Mi coche, la moto…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Combustible", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FUEL_TYPES.forEach { f ->
                        FilterChip(selected = fuel == f.id, onClick = { fuel = f.id }, label = { Text(f.short) })
                    }
                }
                OutlinedTextField(
                    value = tank,
                    onValueChange = { v -> tank = v.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Litros que cargas al llenar") },
                    suffix = { Text("L") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cons,
                    onValueChange = { v -> cons = v.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Consumo aproximado") },
                    suffix = { Text("L/100 km") },
                    supportingText = { Text("Se usa hasta que haya historial de cargas.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing != null && canDelete) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Eliminar vehículo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(Vehicle(existing?.id ?: 0, name.trim(), fuel, tankV!!, consV!!))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar «${existing.name}»?") },
            text = { Text("Se borrarán también sus cargas y recordatorios. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(existing) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
