package com.chungaro.combustible.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.alerts.AlertScheduler
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.AppSettings
import com.chungaro.combustible.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ajustes de la app (combustible habitual, alertas, tema). */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsStore = SettingsStore(app)

    var settings by mutableStateOf(settingsStore.load())
        private set

    init {
        // Abre la base de datos para que corra la migración de las versiones con
        // «O meu veículo» (hereda su combustible) y recarga los ajustes.
        viewModelScope.launch {
            val migrated = withContext(Dispatchers.IO) {
                val hadFuel = settingsStore.hasFuel()
                runCatching { AppDatabase.get(app).openHelper.writableDatabase }
                !hadFuel && settingsStore.hasFuel()
            }
            if (migrated) settings = settingsStore.load()
        }
    }

    fun updateSettings(s: AppSettings) {
        settings = s
        settingsStore.save(s)
        AlertScheduler.schedule(getApplication())
    }
}
