package com.chungaro.combustible.prices

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.data.SettingsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class PricesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val stationCache = mutableMapOf<Int, List<Station>>()

    var favorites by mutableStateOf(store.loadFavorites())
        private set
    var favoriteDetails by mutableStateOf<Map<Long, StationDetail>>(emptyMap())
        private set
    var favoritesLoading by mutableStateOf(false)
        private set
    var favoritesError by mutableStateOf<String?>(null)
        private set

    var districtId by mutableStateOf(store.loadDistrict())
        private set
    var stations by mutableStateOf<List<Station>>(emptyList())
        private set
    var stationsLoading by mutableStateOf(false)
        private set
    var stationsError by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")

    var selectedId by mutableStateOf<Long?>(null)
        private set
    var selected by mutableStateOf<StationDetail?>(null)
        private set
    var selectedError by mutableStateOf<String?>(null)
        private set

    init {
        refreshFavorites()
        districtId?.let { loadStations(it) }
    }

    fun refreshFavorites() {
        val ids = favorites
        if (ids.isEmpty() || favoritesLoading) return
        viewModelScope.launch {
            favoritesLoading = true
            favoritesError = null
            val results = ids.map { id -> async { id to runCatching { DgegApi.station(id) } } }.awaitAll()
            val ok = results.mapNotNull { (id, r) -> r.getOrNull()?.let { id to it } }.toMap()
            // Conserva los datos anteriores de las que fallaron.
            favoriteDetails = favoriteDetails.filterKeys { it in favorites } + ok
            if (results.any { it.second.isFailure }) {
                favoritesError = "Não foi possível atualizar alguns preços. Verifica a ligação."
            }
            favoritesLoading = false
        }
    }

    fun selectDistrict(id: Int) {
        districtId = id
        store.saveDistrict(id)
        loadStations(id)
    }

    fun retryStations() {
        districtId?.let { loadStations(it) }
    }

    private fun loadStations(id: Int) {
        stationCache[id]?.let { stations = it; stationsError = null; return }
        viewModelScope.launch {
            stationsLoading = true
            stationsError = null
            stations = emptyList()
            runCatching { DgegApi.stations(id) }
                .onSuccess {
                    stationCache[id] = it
                    if (districtId == id) stations = it
                }
                .onFailure { if (districtId == id) stationsError = "Não foi possível carregar a lista de bombas. Verifica a ligação." }
            stationsLoading = false
        }
    }

    fun openStation(id: Long) {
        selectedId = id
        selected = favoriteDetails[id]
        selectedError = null
        viewModelScope.launch {
            runCatching { DgegApi.station(id) }
                .onSuccess {
                    if (selectedId == id) selected = it
                    if (id in favorites) favoriteDetails = favoriteDetails + (id to it)
                }
                .onFailure { if (selectedId == id) selectedError = "Não foi possível carregar os preços." }
        }
    }

    fun closeStation() {
        selectedId = null
        selected = null
        selectedError = null
    }

    /** Marca o desmarca una bomba como favorita solo con su id (desde el mapa o su ficha). */
    fun toggleFavoriteId(id: Long) {
        if (id in favorites) {
            favorites = favorites - id
            favoriteDetails = favoriteDetails - id
        } else {
            favorites = favorites + id
            refreshFavorites()
        }
        store.saveFavorites(favorites)
    }

    fun toggleFavorite(detail: StationDetail) {
        val id = detail.id
        if (id in favorites) {
            favorites = favorites - id
            favoriteDetails = favoriteDetails - id
        } else {
            favorites = favorites + id
            favoriteDetails = favoriteDetails + (id to detail)
        }
        store.saveFavorites(favorites)
    }
}
