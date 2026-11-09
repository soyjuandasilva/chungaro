package com.chungaro.combustible.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.alerts.AlertScheduler
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Backup
import com.chungaro.combustible.data.Refuel
import com.chungaro.combustible.data.Reminder
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.data.Stats
import com.chungaro.combustible.data.Summary
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.computeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class RefuelViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).refuelDao()
    private val settingsStore = SettingsStore(app)
    private val selected = MutableStateFlow(settingsStore.loadSelectedVehicle())

    var selectedVehicleId by mutableLongStateOf(selected.value)
        private set

    val vehicles: StateFlow<List<Vehicle>> = dao.observeVehicles()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** null mientras se carga la base de datos. */
    val stats: StateFlow<Stats?> = selected
        .flatMapLatest { id -> dao.observeForVehicle(id) }
        .map { computeStats(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminders: StateFlow<List<Reminder>> = selected
        .flatMapLatest { id -> dao.observeReminders(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var settings by mutableStateOf(settingsStore.load())
        private set

    /** Mensaje corto para mostrar tras exportar/importar. */
    var message by mutableStateOf<String?>(null)

    fun updateSettings(s: AppSettings) {
        settings = s
        settingsStore.save(s)
        AlertScheduler.schedule(getApplication())
    }

    fun selectVehicle(id: Long) {
        selectedVehicleId = id
        selected.value = id
        settingsStore.saveSelectedVehicle(id)
    }

    fun saveVehicle(v: Vehicle) {
        viewModelScope.launch {
            val rowId = dao.upsertVehicle(v)
            val id = if (v.id != 0L) v.id else rowId
            if (id > 0) selectVehicle(id)
        }
    }

    fun deleteVehicle(v: Vehicle) {
        viewModelScope.launch {
            dao.deleteVehicle(v.id)
            val remaining = dao.vehicles()
            val next = remaining.firstOrNull()
                ?: Vehicle(name = "O meu carro", fuelTypeId = 3201, tankLiters = 50.0, consumption = 6.5)
                    .let { it.copy(id = dao.upsertVehicle(it)) }
            selectVehicle(next.id)
        }
    }

    fun save(refuel: Refuel) {
        viewModelScope.launch { dao.upsert(refuel) }
    }

    fun delete(refuel: Refuel) {
        viewModelScope.launch { dao.delete(refuel) }
    }

    fun saveReminder(r: Reminder) {
        viewModelScope.launch {
            dao.upsertReminder(r)
            AlertScheduler.schedule(getApplication())
        }
    }

    fun deleteReminder(r: Reminder) {
        viewModelScope.launch { dao.deleteReminder(r) }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            message = try {
                val json = Backup.toJson(dao.vehicles(), dao.allRefuels(), dao.allReminders())
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
                "Cópia guardada"
            } catch (e: Exception) {
                "Não foi possível guardar a cópia: ${e.message}"
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            message = try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)!!.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }
                val c = Backup.fromJson(json)
                dao.replaceAll(c.vehicles, c.refuels, c.reminders)
                selectVehicle(c.vehicles.first().id)
                "Cópia restaurada: ${c.refuels.size} abastecimentos, ${c.vehicles.size} veículo(s)"
            } catch (e: Exception) {
                "Não foi possível restaurar: ${e.message}"
            }
        }
    }
}

val EmptyStats = Stats(emptyList(), Summary())
