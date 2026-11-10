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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Sync
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
    onVehicle: () -> Unit = {},
    onAlerts: () -> Unit = {},
    onCalculator: () -> Unit = {},
    onTour: () -> Unit = {},
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
        modifier = Modifier.appBackground(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { com.chungaro.combustible.nearby.DarkTopBar("Definições", androidx.compose.material.icons.Icons.Outlined.Settings, onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Accesos rápidos
            com.chungaro.combustible.nearby.DarkCard {
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.DirectionsCar, "O meu veículo", vehicle?.name ?: "", onVehicle)
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.NotificationsNone, "Alertas de preço", if (settings.alertsEnabled) "Ativo" else "Desligado", onAlerts)
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.Calculate, "Calculadora de poupança", "", onCalculator)
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.Straighten, "Unidades", (if (settings.unit == "gal") "Galões" else "Litros") + " e " + settings.currency, null)
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.Sync, "Atualização de preços", "Automática", null)
                SettingsRow(androidx.compose.material.icons.Icons.Outlined.HelpOutline, "Como funciona a Atesta", "", onTour)
            }

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

            Section("Cópia de segurança") {
                Text(
                    "Guarda os teus veículos, abastecimentos e lembretes num ficheiro (por exemplo no Google Drive) para não os perderes se mudares de telemóvel ou reinstalares a app.",
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

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AtestaGreenLight)
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClick != null) {
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
