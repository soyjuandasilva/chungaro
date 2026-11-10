package com.chungaro.combustible.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chungaro.combustible.ChungaroApplication
import com.chungaro.combustible.nearby.DEFAULT_FUEL_ID
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.FuelRowState
import com.chungaro.combustible.nearby.HomeScreen
import com.chungaro.combustible.nearby.MapScreen
import com.chungaro.combustible.nearby.NearbyViewModel
import com.chungaro.combustible.prices.PricesScreen
import com.chungaro.combustible.prices.PricesViewModel

/** Valor de editingId para un abastecimiento nuevo. */
private const val NEW = 0L
private const val TAB_HOME = 0
private const val TAB_MAP = 1
private const val TAB_REFUELS = 2
private const val TAB_FAVORITES = 3

/** Valor de editingVehicle para crear uno nuevo. */
private const val NEW_VEHICLE = 0L

@Composable
fun ChungaroApp(
    vm: RefuelViewModel = viewModel(),
    pricesVm: PricesViewModel = viewModel(),
    nearbyVm: NearbyViewModel = viewModel(),
) {
    val loaded by vm.stats.collectAsStateWithLifecycle()
    val vehicles by vm.vehicles.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val stats = loaded ?: EmptyStats
    val vehicle = vehicles.firstOrNull { it.id == vm.selectedVehicleId } ?: vehicles.firstOrNull()

    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingVehicle by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showVehicles by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var crashReport by rememberSaveable { mutableStateOf(ChungaroApplication.takeLastCrash(context)) }

    // Consumo real (historial) o el estimado del vehículo, en L/100 km.
    val consumptionL100 = stats.summary.avgEfficiency
        ?.takeIf { vm.settings.unit == "L" && it > 0 }
        ?.let { 100.0 / it }
        ?: vehicle?.consumption ?: 6.5
    val myFuelId = vehicle?.fuelTypeId ?: DEFAULT_FUEL_ID

    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            NavigationBarItem(
                selected = tab == TAB_HOME,
                onClick = { tab = TAB_HOME },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                label = { Text("Início", maxLines = 1) },
            )
            NavigationBarItem(
                selected = tab == TAB_MAP,
                onClick = { tab = TAB_MAP },
                icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                label = { Text("Mapa", maxLines = 1) },
            )
            NavigationBarItem(
                selected = tab == TAB_REFUELS,
                onClick = { tab = TAB_REFUELS },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text("Registos", maxLines = 1) },
            )
            NavigationBarItem(
                selected = tab == TAB_FAVORITES,
                onClick = { tab = TAB_FAVORITES },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                label = { Text("Favoritos", maxLines = 1) },
            )
        }
    }

    val id = editingId
    val editVehicleId = editingVehicle
    when {
        editVehicleId != null -> {
            BackHandler { editingVehicle = null }
            VehicleEditScreen(
                existing = vehicles.firstOrNull { it.id == editVehicleId },
                canDelete = vehicles.size > 1,
                onSave = { vm.saveVehicle(it); editingVehicle = null },
                onDelete = { vm.deleteVehicle(it); editingVehicle = null },
                onBack = { editingVehicle = null },
            )
        }
        showVehicles -> {
            BackHandler { showVehicles = false }
            VehiclesScreen(
                vehicles = vehicles,
                selectedId = vehicle?.id ?: 0L,
                onSelect = { vm.selectVehicle(it) },
                onEdit = { editingVehicle = it },
                onAdd = { editingVehicle = NEW_VEHICLE },
                onBack = { showVehicles = false },
            )
        }
        showSettings -> {
            BackHandler { showSettings = false; vm.message = null }
            SettingsScreen(
                settings = vm.settings,
                vehicle = vehicle,
                message = vm.message,
                onChange = { vm.updateSettings(it) },
                onExport = { vm.exportTo(it) },
                onManageVehicles = { showVehicles = true },
                onImport = { vm.importFrom(it) },
                onBack = { showSettings = false; vm.message = null },
            )
        }
        id != null && loaded != null -> {
            BackHandler { editingId = null }
            val existing = stats.rows.firstOrNull { it.refuel.id == id }?.refuel
            val latest = stats.rows.lastOrNull()?.refuel
            val suggestions = if (vm.settings.unit == "L") {
                // Precios de hoy: tu combustible primero, luego el resto y tus favoritas.
                val nearby = FUEL_TYPES.sortedByDescending { it.id == myFuelId }.mapNotNull { f ->
                    val near = (nearbyVm.rows[f.id] as? FuelRowState.Ready)?.result?.nearest ?: return@mapNotNull null
                    PriceSuggestion("Mais próxima · ${f.short}", near.station.price)
                }
                val favorites = pricesVm.favorites.mapNotNull { pricesVm.favoriteDetails[it] }.flatMap { d ->
                    d.fuels.mapNotNull { f ->
                        f.price?.let { PriceSuggestion("${d.brand.ifBlank { d.name }} · ${f.fuel}", it) }
                    }
                }
                nearby + favorites
            } else emptyList()
            RefuelFormScreen(
                existing = if (id == NEW) null else existing,
                lastOdometer = if (id == NEW) latest?.odometer else null,
                settings = vm.settings,
                priceSuggestions = suggestions,
                vehicles = vehicles,
                initialVehicleId = vehicle?.id ?: 1L,
                onSave = {
                    vm.save(it)
                    if (it.vehicleId != vm.selectedVehicleId) vm.selectVehicle(it.vehicleId)
                    editingId = null
                },
                onDelete = { vm.delete(it); editingId = null },
                onBack = { editingId = null },
            )
        }
        tab == TAB_MAP -> {
            BackHandler { tab = TAB_HOME }
            MapScreen(nearbyVm, myFuelId, vehicle?.tankLiters ?: 50.0, bottomBar)
        }
        tab == TAB_FAVORITES -> {
            BackHandler { tab = TAB_HOME }
            PricesScreen(pricesVm, bottomBar)
        }
        tab == TAB_REFUELS -> {
            BackHandler { tab = TAB_HOME }
            MainScreen(
                stats = stats,
                loading = loaded == null,
                settings = vm.settings,
                vehicles = vehicles,
                vehicle = vehicle,
                reminders = reminders,
                onAdd = { editingId = NEW },
                onOpen = { editingId = it.id },
                onOpenSettings = { showSettings = true },
                onSelectVehicle = { vm.selectVehicle(it) },
            extraTips = maintenanceTips(reminders, stats.rows.lastOrNull()?.refuel?.odometer),
                onEditVehicle = { editingVehicle = it?.id ?: NEW_VEHICLE },
                onManageVehicles = { showVehicles = true },
                onSaveReminder = { vm.saveReminder(it) },
                onDeleteReminder = { vm.deleteReminder(it) },
                bottomBar = bottomBar,
            )
        }
        else -> HomeScreen(
            vm = nearbyVm,
            myFuelId = myFuelId,
            tankLiters = vehicle?.tankLiters ?: 50.0,
            consumptionL100 = consumptionL100,
            vehicles = vehicles,
            selectedVehicleId = vehicle?.id ?: 0L,
            onSelectVehicle = { vm.selectVehicle(it) },
            extraTips = maintenanceTips(reminders, stats.rows.lastOrNull()?.refuel?.odometer),
            onOpenSettings = { showSettings = true },
            bottomBar = bottomBar,
        )
    }

    crashReport?.let { report ->
        CrashDialog(report, onDismiss = { crashReport = null })
    }
}

/** Recordatorios de mantenimiento próximos o vencidos, como consejos del ticker. */
private fun maintenanceTips(
    reminders: List<com.chungaro.combustible.data.Reminder>,
    odometer: Double?,
): List<com.chungaro.combustible.nearby.Tip> {
    val today = java.time.LocalDate.now().toEpochDay()
    return reminders.filter { !it.done }.mapNotNull { r ->
        val days = r.dueEpochDay?.let { it - today }
        val km = if (r.dueOdometer != null && odometer != null) r.dueOdometer - odometer else null
        val soon = (days != null && days <= 7) || (km != null && km <= 500)
        if (!soon) return@mapNotNull null
        val overdue = (days != null && days < 0) || (km != null && km <= 0)
        com.chungaro.combustible.nearby.Tip(
            emoji = "🔧",
            text = "${r.title}: " + com.chungaro.combustible.alerts.reminderStatus(r.dueEpochDay, r.dueOdometer, today, odometer).lowercase(),
            color = if (overdue) 0xFFDC2626 else 0xFFD97706,
        )
    }
}
