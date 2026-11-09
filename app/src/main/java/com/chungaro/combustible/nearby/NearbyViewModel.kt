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

sealed interface NearbyState {
    data object Idle : NearbyState
    data object Loading : NearbyState
    data class Error(val message: String) : NearbyState
    data class Ready(val result: NearbyResult) : NearbyState
}

class NearbyViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val cache = mutableMapOf<Int, Pair<Long, List<PricedStation>>>()
    private var job: Job? = null

    var fuelId by mutableIntStateOf(store.loadNearbyFuel())
        private set
    var radiusKm by mutableIntStateOf(store.loadRadius())
        private set
    var state by mutableStateOf<NearbyState>(NearbyState.Idle)
        private set
    private var location: Location? = null

    fun refresh(force: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            state = NearbyState.Loading
            val loc = LocationHelper.current(getApplication<Application>())
            if (loc == null) {
                state = NearbyState.Error("No se pudo obtener tu ubicación. Activa la ubicación del teléfono y vuelve a intentarlo.")
                return@launch
            }
            location = loc
            val fuel = fuelId
            val stations = try {
                stationsFor(fuel, force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = NearbyState.Error("No se pudieron descargar los precios. Revisa tu conexión.")
                return@launch
            }
            recompute(stations)
        }
    }

    fun selectFuel(id: Int) {
        if (id == fuelId) return
        fuelId = id
        store.saveNearbyFuel(id)
        refresh()
    }

    fun selectRadius(km: Int) {
        if (km == radiusKm) return
        radiusKm = km
        store.saveRadius(km)
        val cached = cache[fuelId]?.second
        if (cached != null && location != null) recompute(cached) else refresh()
    }

    private fun recompute(stations: List<PricedStation>) {
        val loc = location ?: return
        state = NearbyState.Ready(computeNearby(stations, loc.latitude, loc.longitude, radiusKm.toDouble()))
    }

    private suspend fun stationsFor(fuel: Int, force: Boolean): List<PricedStation> {
        val now = System.currentTimeMillis()
        val hit = cache[fuel]
        if (!force && hit != null && now - hit.first < CACHE_MS) return hit.second
        val fresh = DgegApi.pricedStations(fuel)
        cache[fuel] = now to fresh
        return fresh
    }

    companion object {
        private const val CACHE_MS = 30 * 60_000L
        val RADIUS_OPTIONS = listOf(5, 10, 20)
    }
}
