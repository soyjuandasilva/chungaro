package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.Atesta
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chungaro.combustible.ui.appBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.ui.AtestaBorder
import com.chungaro.combustible.ui.AtestaCard
import com.chungaro.combustible.ui.AtestaGreenLight
import java.util.Locale

/** Botones − valor + (litros, precio objetivo…). */
@Composable
private fun Stepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = AtestaCard, border = BorderStroke(1.dp, AtestaBorder)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = "Menos", tint = AtestaGreenLight) }
            Text(text, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(76.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = "Mais", tint = AtestaGreenLight) }
        }
    }
}

// ---------------------------------------------------------------- Calculadora

/**
 * «Calculadora de poupança»: cuánto pagas por [tankLiters] litros en los postos
 * más baratos y en el más próximo; eliges uno y ves cuánto ahorras frente al más caro.
 */
@Composable
fun SavingsCalculatorScreen(vm: NearbyViewModel, fuelId: Int, tankLiters: Double, onBack: () -> Unit, onOpenStation: (Long) -> Unit) {
    var liters by rememberSaveable { mutableIntStateOf(tankLiters.toInt().coerceIn(5, 200)) }
    var fuel by rememberSaveable { mutableIntStateOf(fuelId) }
    var picked by rememberSaveable { mutableStateOf<Long?>(null) }
    val f = FUEL_TYPES.first { it.id == fuel }
    val r = (vm.rows[fuel] as? FuelRowState.Ready)?.result
    // Las 4 más baratas del radio y la más próxima, sin repetir
    val options = remember(r) {
        (r?.cheapestList.orEmpty().take(4) + listOfNotNull(r?.nearest)).distinctBy { it.station.id }.sortedBy { it.station.price }
    }
    val sel = options.firstOrNull { it.station.id == picked } ?: options.firstOrNull()
    val worst = options.maxByOrNull { it.station.price }

    Scaffold(modifier = Modifier.appBackground(), containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, topBar = { DarkTopBar("Calculadora de poupança", Icons.Outlined.Calculate, onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Litros a abastecer", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Stepper("$liters L", onMinus = { liters = (liters - 5).coerceAtLeast(5) }, onPlus = { liters = (liters + 5).coerceAtMost(200) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Combustível", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                DropPill(f.short, FUEL_TYPES.map { it.short }, FUEL_TYPES.indexOf(f), selected = true, label = "Combustível") { fuel = FUEL_TYPES[it].id; picked = null }
            }
            if (options.isEmpty()) {
                if (vm.rows[fuel] is FuelRowState.Error) {
                    DarkCard { ErrorRetry("Sem ligação: não conseguimos descarregar os preços.", onRetry = { vm.refresh(force = true) }) }
                } else {
                    Text("Sem preços na tua zona ainda. Ativa a localização no mapa.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                DarkCard {
                    options.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(color = AtestaBorder)
                        val isSel = s.station.id == sel?.station?.id
                        StationRow(
                            s.station, s.km,
                            onClick = { picked = s.station.id },
                            subtitle = "${euro3(s.station.price)} / L · ${formatKm(s.km)}",
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatMoney(s.station.price * liters), fontWeight = FontWeight.Bold)
                            }
                            RadioButton(selected = isSel, onClick = { picked = s.station.id })
                        }
                    }
                }
                if (sel != null && worst != null) {
                    val saving = (worst.station.price - sel.station.price) * liters
                    Surface(
                        shape = CardShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(36.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    if (saving > 0.005) "Poupas ${formatMoney(saving)}" else "É o mais caro da lista",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    if (saving > 0.005) "no ${stationTitle(sel.station)} (vs. ${stationTitle(worst.station)})" else "Escolhe outro posto para poupar.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { onOpenStation(sel.station.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("Ver o posto escolhido", fontWeight = FontWeight.Bold) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text("Diferença para $liters litros. Valores indicativos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- Alertas

/** «Alertas de preço»: combustível, preço-alvo e onde aplicar o aviso. */
@Composable
fun AlertsScreen(
    settings: AppSettings,
    defaultFuelId: Int,
    favoritesCount: Int,
    canNotify: Boolean,
    onRequestNotifications: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var fuel by rememberSaveable { mutableIntStateOf(settings.alertFuelId ?: defaultFuelId) }
    var target by rememberSaveable { mutableDoubleStateOf(settings.alertThreshold ?: 1.650) }
    var favoritesOnly by rememberSaveable { mutableStateOf(settings.alertFavoritesOnly) }
    var saved by remember { mutableStateOf(false) }
    val f = FUEL_TYPES.first { it.id == fuel }

    Scaffold(modifier = Modifier.appBackground(), containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, topBar = { DarkTopBar("Alertas de preço", Icons.Outlined.NotificationsNone, onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DarkCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BlockTitle(if (settings.alertsEnabled) "Alerta ativo" else "Criar alerta", Modifier.weight(1f))
                        Switch(
                            checked = settings.alertsEnabled,
                            onCheckedChange = { on -> if (on && !canNotify) onRequestNotifications() else onSave(settings.copy(alertsEnabled = on)) },
                            modifier = Modifier.semantics { contentDescription = "Alerta de preço ligado" },
                        )
                    }
                    HorizontalDivider(color = Atesta.colors.cardBorder)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Combustível", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        var open by remember { mutableStateOf(false) }
                        Box {
                            FilterPill(f.short, onClick = { open = true }, description = "Combustível: ${f.short}") {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                FUEL_TYPES.forEach { x -> DropdownMenuItem(text = { Text(x.short) }, onClick = { open = false; fuel = x.id }) }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Preço-alvo (€/L)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Stepper(
                            String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f", target),
                            onMinus = { target = ((target - 0.005) * 1000).toLong() / 1000.0 },
                            onPlus = { target = ((target + 0.005) * 1000).toLong() / 1000.0 },
                        )
                    }
                }
            }
            BlockTitle("Aplicar em")
            DarkCard(Modifier.selectableGroup()) {
                listOf(false to "Todas as bombas da tua zona", true to "Apenas nas favoritas ($favoritesCount)").forEach { (fav, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = favoritesOnly == fav, role = Role.RadioButton, onClick = { favoritesOnly = fav })
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = favoritesOnly == fav, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Button(
                onClick = {
                    if (!canNotify) onRequestNotifications()
                    onSave(settings.copy(alertsEnabled = true, alertFuelId = fuel, alertThreshold = target, alertFavoritesOnly = favoritesOnly))
                    saved = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar alerta", fontWeight = FontWeight.Bold)
            }
            if (saved) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Atesta.colors.accent, modifier = Modifier.size(20.dp))
                    Text(
                        "Alerta guardado: avisamos-te quando o ${f.short} descer de ${String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f €", target)}" +
                            (if (favoritesOnly) " numa favorita." else " perto de ti.") + " Verificamos a cada 6 horas.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
