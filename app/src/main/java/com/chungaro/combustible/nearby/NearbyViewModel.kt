package com.chungaro.combustible.nearby

import android.app.Application
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.prices.DgegApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface FuelRowState {
    data object Loading : FuelRowState
    data class Error(val message: String) : FuelRowState
    data class Ready(val result: NearbyResult) : FuelRowState
}

class NearbyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val cache = mutableMapOf<Int, Pair<Long, List<PricedStation>>>()
    private val downloads = Semaphore(3)
    private var job: Job? = null
    private var location: Location? = null

    var radiusKm by mutableIntStateOf(store.loadRadius())
        private set
    var locating by mutableStateOf(false)
        private set
    var locationError by mutableStateOf<String?>(null)
        private set
    var rows by mutableStateOf<Map<Int, FuelRowState>>(emptyMap())
        private set
    var updatedAt by mutableStateOf<Long?>(null)
        private set

    val started: Boolean get() = job != null

    fun refresh(force: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            locating = true
            locationError = null
            val loc = LocationHelper.current(getApplication<Application>())
            locating = false
            if (loc == null) {
                locationError = "No se pudo obtener tu ubicación. Activa la ubicación del teléfono y vuelve a intentarlo."
                return@launch
            }
            location = loc
            rows = FUEL_TYPES.associate { f ->
                val cached = cache[f.id]?.second
                f.id to (if (cached != null && !force) FuelRowState.Ready(compute(cached, loc)) else FuelRowState.Loading)
            }
            updatedAt = System.currentTimeMillis()
            FUEL_TYPES.forEach { f ->
                launch {
                    val state = try {
                        FuelRowState.Ready(compute(stationsFor(f.id, force), loc))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        FuelRowState.Error("Sin conexión")
                    }
                    rows = rows + (f.id to state)
                }
            }
        }
    }

    fun selectRadius(km: Int) {
        if (km == radiusKm) return
        radiusKm = km
        store.saveRadius(km)
        val loc = location ?: return
        rows = rows.mapValues { (id, state) ->
            val cached = cache[id]?.second
            if (state is FuelRowState.Ready && cached != null) FuelRowState.Ready(compute(cached, loc)) else state
        }
    }

    private fun compute(stations: List<PricedStation>, loc: Location) =
        computeNearby(stations, loc.latitude, loc.longitude, radiusKm.toDouble())

    private suspend fun stationsFor(fuel: Int, force: Boolean): List<PricedStation> {
        val now = System.currentTimeMillis()
        val hit = cache[fuel]
        if (!force && hit != null && now - hit.first < CACHE_MS) return hit.second
        val fresh = downloads.withPermit { DgegApi.pricedStations(fuel) }
        cache[fuel] = now to fresh
        return fresh
    }

    companion object {
        private const val CACHE_MS = 30 * 60_000L
        val RADIUS_OPTIONS = listOf(5, 10, 20)
    }
}
