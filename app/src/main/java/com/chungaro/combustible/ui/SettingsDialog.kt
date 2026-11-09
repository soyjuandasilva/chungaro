package com.chungaro.combustible.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings

@Composable
fun SettingsDialog(current: AppSettings, onDismiss: () -> Unit, onSave: (AppSettings) -> Unit) {
    var unit by remember { mutableStateOf(current.unit) }
    var currency by remember { mutableStateOf(current.currency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes") },
        text = {
            Column {
                Text("Unidad de combustible", style = MaterialTheme.typography.labelLarge)
                listOf("L" to "Litros", "gal" to "Galones").forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = unit == value, onClick = { unit = value })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = unit == value, onClick = { unit = value })
                        Text(label)
                    }
                }
                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it.take(4) },
                    label = { Text("Símbolo de moneda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "Cambiar la unidad no convierte los valores ya guardados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(AppSettings(unit, currency.trim().ifEmpty { "$" })) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
