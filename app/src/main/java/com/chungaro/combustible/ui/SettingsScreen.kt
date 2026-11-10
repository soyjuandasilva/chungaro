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
import com.chungaro.combustible.R
import androidx.compose.ui.res.stringResource
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
    onManageVehicles: () -> Unit,
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
                title = { Text("Definições", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Veículos") {
                Text(
                    "Tipo, marca, modelo, ano, combustível e consumo de cada veículo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onManageVehicles, modifier = Modifier.fillMaxWidth()) { Text("Os meus veículos") }
            }

            Section("Unidades") {
                Label("Combustível")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings.unit == "L", onClick = { onChange(settings.copy(unit = "L")) }, label = { Text("Litros") })
                    FilterChip(selected = settings.unit == "gal", onClick = { onChange(settings.copy(unit = "gal")) }, label = { Text("Galões") })
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
                    label = { Text("Símbolo da moeda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Alertas de preço") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Avisar-me quando o preço descer", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "De ${fuel?.short ?: "o seu combustível"} (o de ${vehicle?.name ?: "o seu veículo"}), perto da sua última localização. Verificado a cada 6 horas.",
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
                    label = { Text("Avisar se descer abaixo de") },
                    suffix = { Text("€/L") },
                    placeholder = { Text("1.650") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Cópia de segurança") {
                Text(
                    "Guarde os seus veículos, abastecimentos e lembretes num ficheiro (por exemplo no Google Drive) para não os perder se mudar de telemóvel ou reinstalar a app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("atesta-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) {
                        Text("Exportar")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("Importar")
                    }
                }
                Text(
                    "Importar substitui todos os dados atuais pelos do ficheiro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                message?.let { Text(it, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            }

            Text(
                stringResource(R.string.app_name) + " ${BuildConfig.VERSION_NAME} · Preços: DGEG · Mapa: © OpenStreetMap\n" +
                    "Os logótipos e marcas pertencem aos respetivos proprietários e são usados apenas para identificar as bombas.",
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
