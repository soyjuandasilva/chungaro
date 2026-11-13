package com.chungaro.combustible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.BuildConfig
import com.chungaro.combustible.R
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.nearby.BlockTitle
import com.chungaro.combustible.nearby.DarkCard
import com.chungaro.combustible.nearby.DarkTopBar
import com.chungaro.combustible.nearby.Dot
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.uiColor

/** Definições: combustível habitual, aparência, atalhos e informação da app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onAlerts: () -> Unit,
    onCalculator: () -> Unit,
    onTour: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.appBackground(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { DarkTopBar("Definições", Icons.Outlined.Settings, onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("O teu combustível") {
                Hint("É o que aparece primeiro no mapa, nos preços e nos alertas. Podes mudá-lo a qualquer momento.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FUEL_TYPES.forEach { f ->
                        FilterChip(
                            selected = settings.fuelId == f.id,
                            onClick = { onChange(settings.copy(fuelId = f.id)) },
                            label = { Text(f.short) },
                            leadingIcon = { Dot(f.uiColor, 10.dp) },
                        )
                    }
                }
            }

            Section("Aparência") {
                Hint("O tema claro lê-se melhor ao sol; o escuro, à noite. Automático segue o telemóvel.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.AUTO to "Automático", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro").forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onChange(settings.copy(themeMode = mode)) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            DarkCard {
                SettingsRow(Icons.Outlined.NotificationsNone, "Alertas de preço", if (settings.alertsEnabled) "Ativos" else "Desligados", onAlerts)
                HorizontalDivider(color = AbasteceHoje.colors.cardBorder)
                SettingsRow(Icons.Outlined.Calculate, "Calculadora de poupança", null, onCalculator)
                HorizontalDivider(color = AbasteceHoje.colors.cardBorder)
                SettingsRow(Icons.AutoMirrored.Outlined.HelpOutline, "Rever a apresentação", null, onTour)
                HorizontalDivider(color = AbasteceHoje.colors.cardBorder)
                SettingsRow(Icons.Outlined.Sync, "Atualização dos preços", "A cada 10 minutos", null)
            }

            Text(
                stringResource(R.string.app_name) + " ${BuildConfig.VERSION_NAME} · Preços: DGEG · Mapa: © OpenStreetMap\n" +
                    "Os logótipos e marcas pertencem aos respetivos donos e servem apenas para identificar os postos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    DarkCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BlockTitle(title)
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, value: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AbasteceHoje.colors.accent)
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 14.dp))
        value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
