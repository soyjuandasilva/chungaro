package com.chungaro.combustible.nearby

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.PricePoint
import com.chungaro.combustible.data.SettingsStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import com.chungaro.combustible.news.NewsApi
import com.chungaro.combustible.news.NewsItem
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
    /** Momento de la última descarga de precios. */
    var pricesTime by mutableStateOf<Long?>(null)
        private set

    val started: Boolean get() = job != null

    /** Cambia cada vez que llegan precios nuevos (para recalcular las medias del país). */
    var pricesVersion by mutableIntStateOf(0)
        private set

    /** Noticias sobre el precio de la próxima semana (Google News). */
    var news by mutableStateOf<List<NewsItem>>(emptyList())
        private set
    var newsLoading by mutableStateOf(false)
        private set
    var newsError by mutableStateOf(false)
        private set
    private var newsTime = 0L

    /** Horario de cada bomba (líneas ya formateadas; vacía si OSM no lo tiene). Sin clave = cargando. */
    var openingHours by mutableStateOf<Map<Long, List<String>>>(emptyMap())
        private set
    private val hoursRequested = mutableSetOf<Long>()

    fun loadOpeningHours(station: PricedStation) {
        if (!hoursRequested.add(station.id)) return
        viewModelScope.launch {
            val lines = try {
                OpeningHours.lookup(station.lat, station.lon)?.let { OpeningHours.format(it) }.orEmpty()
            } catch (e: CancellationException) {
                hoursRequested.remove(station.id)
                throw e
            } catch (e: Exception) {
                hoursRequested.remove(station.id) // se reintenta la próxima vez
                emptyList()
            }
            openingHours = openingHours + (station.id to lines)
        }
    }

    fun loadNews(force: Boolean = false) {
        if (newsLoading) return
        if (!force && news.isNotEmpty() && System.currentTimeMillis() - newsTime < NEWS_CACHE_MS) return
        newsLoading = true
        viewModelScope.launch {
            try {
                news = NewsApi.latest(5)
                newsTime = System.currentTimeMillis()
                newsError = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                newsError = true
            } finally {
                newsLoading = false
            }
        }
    }

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
            pricesVersion++
            showCached()

            // 2) Ubicación actual.
            locating = true
            locationError = null
            val loc = LocationHelper.current(app)
            locating = false
            if (loc == null) {
                locationError = if (location != null) {
                    "Não conseguimos atualizar a tua localização; usamos a última conhecida."
                } else {
                    "Não conseguimos saber onde estás. Ativa a localização do telemóvel e tenta outra vez."
                }
                // Sin ubicación se siguen descargando precios: el precio medio del país no la necesita.
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
                        pricesVersion++
                        withContext(Dispatchers.IO) { PriceCache.save(app, f.id, fresh) }
                        location?.let { (lat, lon) -> HistoryRecorder.record(app, f.id, fresh, lat, lon) }
                        updateRow(f.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (rows[f.id] !is FuelRowState.Ready) rows = rows + (f.id to FuelRowState.Error("Sem ligação"))
                    }
                }
            }
        }
    }

    private val historyDao = AppDatabase.get(app).historyDao()

    /** Histórico de los últimos 30 días de una bomba (o de la zona: HistoryRecorder.ZONE_AVG). */
    fun history(fuelId: Int, stationId: Long): Flow<List<PricePoint>> =
        historyDao.observe(fuelId, stationId, LocalDate.now().toEpochDay() - 30)

    /** Precio medio actual en todo el país (todas las bombas con precio). */
    fun nationalPrice(fuelId: Int): NationalPrice? = cache[fuelId]?.second?.let { nationalAverage(it) }

    /** Histórico de todas las bombas de un combustible (para comparar semanas). */
    fun stationsHistory(fuelId: Int): Flow<List<PricePoint>> =
        historyDao.observeStations(fuelId, LocalDate.now().toEpochDay() - 15)

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
        pricesTime = cache.values.maxOfOrNull { it.first }
    }

    companion object {
        private const val CACHE_MS = 10 * 60_000L
        private const val NEWS_CACHE_MS = 60 * 60_000L
        val RADIUS_OPTIONS = listOf(5, 10, 20)
    }
}
