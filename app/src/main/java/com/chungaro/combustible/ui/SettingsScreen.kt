package com.chungaro.combustible.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.BuildConfig
import com.chungaro.combustible.alerts.Notifier
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.nearby.FUEL_TYPES
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    vehicle: Vehicle?,
    message: String?,
    onChange: (AppSettings) -> Unit,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (granted) onChange(settings.copy(alertsEnabled = true))
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }
    var threshold by remember { mutableStateOf(settings.alertThreshold?.let { editableNumber(it) } ?: "") }
    var currency by remember { mutableStateOf(settings.currency) }
    val fuel = FUEL_TYPES.firstOrNull { it.id == vehicle?.fuelTypeId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Unidades") {
                Label("Combustible")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings.unit == "L", onClick = { onChange(settings.copy(unit = "L")) }, label = { Text("Litros") })
                    FilterChip(selected = settings.unit == "gal", onClick = { onChange(settings.copy(unit = "gal")) }, label = { Text("Galones") })
                }
                Label("Consumo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.consumptionUnit == "L100",
                        onClick = { onChange(settings.copy(consumptionUnit = "L100")) },
                        label = { Text("L/100 km") },
                    )
                    FilterChip(
                        selected = settings.consumptionUnit == "KML",
                        onClick = { onChange(settings.copy(consumptionUnit = "KML")) },
                        label = { Text("km/L") },
                    )
                }
                OutlinedTextField(
                    value = currency,
                    onValueChange = {
                        currency = it.take(4)
                        if (currency.isNotBlank()) onChange(settings.copy(currency = currency.trim()))
                    },
                    label = { Text("Símbolo de moneda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Avisos de precio") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisarme cuando baje el precio", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "De ${fuel?.short ?: "tu combustible"} (el de ${vehicle?.name ?: "tu vehículo"}), cerca de tu última ubicación. Se revisa cada 6 horas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.alertsEnabled && canNotify,
                        onCheckedChange = { on ->
                            if (on && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onChange(settings.copy(alertsEnabled = on))
                            }
                        },
                    )
                }
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { v ->
                        threshold = v.filter { it.isDigit() || it == '.' || it == ',' }
                        onChange(settings.copy(alertThreshold = parseNumber(threshold)?.takeIf { it > 0 }))
                    },
                    label = { Text("Avisar si baja de") },
                    suffix = { Text("€/L") },
                    placeholder = { Text("1.650") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Copia de seguridad") {
                Text(
                    "Guarda tus vehículos, cargas y recordatorios en un archivo (por ejemplo en Google Drive) para no perderlos si cambias de móvil o reinstalas la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("chungaro-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) {
                        Text("Exportar")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("Importar")
                    }
                }
                Text(
                    "Importar reemplaza todos los datos actuales por los del archivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                message?.let { Text(it, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            }

            Text(
                "Chungaro ${BuildConfig.VERSION_NAME} · Precios: DGEG · Mapa: © OpenStreetMap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}
