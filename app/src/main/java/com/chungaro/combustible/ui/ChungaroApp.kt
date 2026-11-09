package com.chungaro.combustible.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.chungaro.combustible.ChungaroApplication
import com.chungaro.combustible.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chungaro.combustible.prices.PricesScreen
import com.chungaro.combustible.prices.PricesViewModel

/** Valor de editingId para un abastecimiento nuevo. */
private const val NEW = 0L
private const val TAB_REFUELS = 0
private const val TAB_PRICES = 1

@Composable
fun ChungaroApp(
    vm: RefuelViewModel = viewModel(),
    pricesVm: PricesViewModel = viewModel(),
) {
    val loaded by vm.stats.collectAsStateWithLifecycle()
    val stats = loaded ?: EmptyStats
    var tab by rememberSaveable { mutableIntStateOf(TAB_REFUELS) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var crashReport by rememberSaveable { mutableStateOf(ChungaroApplication.takeLastCrash(context)) }

    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            NavigationBarItem(
                selected = tab == TAB_REFUELS,
                onClick = { tab = TAB_REFUELS },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text("Mis cargas") },
            )
            NavigationBarItem(
                selected = tab == TAB_PRICES,
                onClick = { tab = TAB_PRICES },
                icon = { Icon(painterResource(R.drawable.ic_fuel), contentDescription = null) },
                label = { Text("Precios") },
            )
        }
    }

    val id = editingId
    when {
        id != null && loaded != null -> {
            BackHandler { editingId = null }
            val existing = stats.rows.firstOrNull { it.refuel.id == id }?.refuel
            val latest = stats.rows.lastOrNull()?.refuel
            val suggestions = if (vm.settings.unit == "L") {
                pricesVm.favorites.mapNotNull { pricesVm.favoriteDetails[it] }.flatMap { d ->
                    d.fuels.mapNotNull { f ->
                        f.price?.let { PriceSuggestion("${d.brand.ifBlank { d.name }} · ${f.fuel}", it) }
                    }
                }
            } else emptyList()
            RefuelFormScreen(
                existing = if (id == NEW) null else existing,
                lastOdometer = if (id == NEW) latest?.odometer else null,
                settings = vm.settings,
                priceSuggestions = suggestions,
                onSave = { vm.save(it); editingId = null },
                onDelete = { vm.delete(it); editingId = null },
                onBack = { editingId = null },
            )
        }
        tab == TAB_PRICES -> {
            BackHandler { tab = TAB_REFUELS }
            PricesScreen(pricesVm, bottomBar)
        }
        else -> MainScreen(
            stats = stats,
            loading = loaded == null,
            settings = vm.settings,
            onAdd = { editingId = NEW },
            onOpen = { editingId = it.id },
            onOpenSettings = { showSettings = true },
            bottomBar = bottomBar,
        )
    }

    crashReport?.let { report ->
        CrashDialog(report, onDismiss = { crashReport = null })
    }

    if (showSettings) {
        SettingsDialog(
            current = vm.settings,
            onDismiss = { showSettings = false },
            onSave = { vm.updateSettings(it); showSettings = false },
        )
    }
}
