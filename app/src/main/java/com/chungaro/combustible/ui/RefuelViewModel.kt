package com.chungaro.combustible.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.Refuel
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.data.Stats
import com.chungaro.combustible.data.Summary
import com.chungaro.combustible.data.computeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RefuelViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).refuelDao()
    private val settingsStore = SettingsStore(app)

    /** null mientras se carga la base de datos. */
    val stats: StateFlow<Stats?> = dao.observeAll()
        .map { computeStats(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var settings by mutableStateOf(settingsStore.load())
        private set

    fun updateSettings(s: AppSettings) {
        settings = s
        settingsStore.save(s)
    }

    fun save(refuel: Refuel) {
        viewModelScope.launch { dao.upsert(refuel) }
    }

    fun delete(refuel: Refuel) {
        viewModelScope.launch { dao.delete(refuel) }
    }
}

val EmptyStats = Stats(emptyList(), Summary())
