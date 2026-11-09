package com.chungaro.combustible.nearby

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.prices.DgegApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface FuelRowState {
    data object Loading : FuelRowState
    data class Error(val message: String) : FuelRowState
    data class Ready(val result: NearbyResult) : FuelRowState
}

class NearbyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    /** fuelId -> (momento de descarga, gasolineras). */
    private val cache = mutableMapOf<Int, Pair<Long, List<PricedStation>>>()
    private val downloads = Semaphore(3)
    private var job: Job? = null

    /** (lat, lon) usada en los cálculos. */
    var location by mutableStateOf<Pair<Double, Double>?>(null)
        private set
    var radiusKm by mutableIntStateOf(store.loadRadius())
        private set
    var locating by mutableStateOf(false)
        private set
    var locationError by mutableStateOf<String?>(null)
        private set
    var rows by mutableStateOf<Map<Int, FuelRowState>>(emptyMap())
        private set
    /** Momento de los precios mostrados (el más antiguo de los combustibles). */
    var pricesTime by mutableStateOf<Long?>(null)
        private set

    val started: Boolean get() = job != null

    /** Gasolineras con precio del combustible [fuelId], si ya se descargaron. */
    fun stationsOf(fuelId: Int): List<PricedStation>? = cache[fuelId]?.second

    fun refresh(force: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            val app = getApplication<Application>()
            // 1) Al instante: precios guardados + última ubicación conocida.
            if (cache.isEmpty()) {
                withContext(Dispatchers.IO) {
                    FUEL_TYPES.forEach { f -> PriceCache.load(app, f.id)?.let { cache[f.id] = it } }
                }
            }
            if (location == null) location = store.loadLastLocation()
            showCached()

            // 2) Ubicación actual.
            locating = true
            locationError = null
            val loc = LocationHelper.current(app)
            locating = false
            if (loc == null) {
                locationError = if (location != null) {
                    "No se pudo actualizar tu ubicación; se usa la última conocida."
                } else {
                    "No se pudo obtener tu ubicación. Activa la ubicación del teléfono y vuelve a intentarlo."
                }
                if (location == null) return@launch
            } else {
                location = loc.latitude to loc.longitude
                store.saveLastLocation(loc.latitude, loc.longitude)
                showCached()
            }

            // 3) Descarga lo que falte o esté viejo.
            val now = System.currentTimeMillis()
            FUEL_TYPES.forEach { f ->
                val hit = cache[f.id]
                if (!force && hit != null && now - hit.first < CACHE_MS) return@forEach
                if (hit == null) rows = rows + (f.id to FuelRowState.Loading)
                launch {
                    try {
                        val fresh = downloads.withPermit { DgegApi.pricedStations(f.id) }
                        cache[f.id] = System.currentTimeMillis() to fresh
                        withContext(Dispatchers.IO) { PriceCache.save(app, f.id, fresh) }
                        updateRow(f.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (rows[f.id] !is FuelRowState.Ready) rows = rows + (f.id to FuelRowState.Error("Sin conexión"))
                    }
                }
            }
        }
    }

    fun selectRadius(km: Int) {
        if (km == radiusKm) return
        radiusKm = km
        store.saveRadius(km)
        showCached()
    }

    private fun showCached() {
        FUEL_TYPES.forEach { f -> if (cache[f.id] != null) updateRow(f.id) }
    }

    private fun updateRow(fuelId: Int) {
        val (lat, lon) = location ?: return
        val list = cache[fuelId]?.second ?: return
        rows = rows + (fuelId to FuelRowState.Ready(computeNearby(list, lat, lon, radiusKm.toDouble(), listSize = 10)))
        pricesTime = cache.values.minOfOrNull { it.first }
    }

    companion object {
        private const val CACHE_MS = 30 * 60_000L
        val RADIUS_OPTIONS = listOf(5, 10, 20)
    }
}
