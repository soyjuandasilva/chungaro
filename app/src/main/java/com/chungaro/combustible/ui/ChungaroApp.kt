package com.chungaro.combustible.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Valor de [editingId] para un abastecimiento nuevo. */
private const val NEW = 0L

@Composable
fun ChungaroApp(vm: RefuelViewModel = viewModel()) {
    val loaded by vm.stats.collectAsStateWithLifecycle()
    val stats = loaded ?: EmptyStats
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val id = editingId
    if (id != null && loaded != null) {
        BackHandler { editingId = null }
        val existing = stats.rows.firstOrNull { it.refuel.id == id }?.refuel
        val latest = stats.rows.lastOrNull()?.refuel
        RefuelFormScreen(
            existing = if (id == NEW) null else existing,
            lastOdometer = if (id == NEW) latest?.odometer else null,
            settings = vm.settings,
            onSave = { vm.save(it); editingId = null },
            onDelete = { vm.delete(it); editingId = null },
            onBack = { editingId = null },
        )
    } else {
        MainScreen(
            stats = stats,
            loading = loaded == null,
            settings = vm.settings,
            onAdd = { editingId = NEW },
            onOpen = { editingId = it.id },
            onOpenSettings = { showSettings = true },
        )
    }

    if (showSettings) {
        SettingsDialog(
            current = vm.settings,
            onDismiss = { showSettings = false },
            onSave = { vm.updateSettings(it); showSettings = false },
        )
    }
}
