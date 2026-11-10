package com.chungaro.combustible.nearby

import android.content.Context
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.PricePoint
import java.time.LocalDate

enum class Advice { COLLECTING, LOWEST, BUY_NOW_RISING, WAIT_FALLING, STABLE }

data class Trend(
    val last: Double?,
    /** Variación en % respecto a hace ~7 días (o al primer dato). */
    val changePct: Double?,
    val advice: Advice,
    val days: Int,
)

const val MIN_DAYS_FOR_TREND = 3

/** Calcula la tendencia y la recomendación a partir del histórico diario. */
fun computeTrend(points: List<PricePoint>): Trend {
    val sorted = points.sortedBy { it.day }
    if (sorted.isEmpty()) return Trend(null, null, Advice.COLLECTING, 0)
    val last = sorted.last()
    val ref = sorted.lastOrNull { it.day <= last.day - 7 } ?: sorted.first()
    val change = if (sorted.size >= 2 && ref.price > 0) (last.price - ref.price) / ref.price * 100 else null
    if (sorted.size < MIN_DAYS_FOR_TREND) return Trend(last.price, change, Advice.COLLECTING, sorted.size)
    val recent = sorted.filter { it.day >= last.day - 14 }
    val advice = when {
        recent.size >= 5 && last.price <= recent.minOf { it.price } + 0.0005 && (change ?: 0.0) < 0 -> Advice.LOWEST
        (change ?: 0.0) > 1.0 -> Advice.BUY_NOW_RISING
        (change ?: 0.0) < -1.0 -> Advice.WAIT_FALLING
        else -> Advice.STABLE
    }
    return Trend(last.price, change, advice, sorted.size)
}

/** Guarda en el histórico los precios recién descargados alrededor de una ubicación. */
object HistoryRecorder {
    const val ZONE_AVG = 0L
    const val ZONE_MIN = -1L
    /** Media de todas las bombas del país (solo con la descarga completa, en primer plano). */
    const val NATIONAL_AVG = -2L
    /** Radio fijo de la "zona" para que el histórico sea comparable día a día. */
    const val ZONE_KM = 15.0

    fun points(day: Long, fuelId: Int, stations: List<PricedStation>, lat: Double, lon: Double): List<PricePoint> {
        val near = stations.filter { distanceKm(lat, lon, it.lat, it.lon) <= ZONE_KM }
        if (near.isEmpty()) return emptyList()
        return near.map { PricePoint(day, fuelId, it.id, it.price) } +
            PricePoint(day, fuelId, ZONE_AVG, near.sumOf { it.price } / near.size) +
            PricePoint(day, fuelId, ZONE_MIN, near.minOf { it.price })
    }

    suspend fun record(context: Context, fuelId: Int, stations: List<PricedStation>, lat: Double, lon: Double) {
        runCatching {
            val today = LocalDate.now().toEpochDay()
            val pts = points(today, fuelId, stations, lat, lon)
            if (pts.isEmpty()) return
            val dao = AppDatabase.get(context).historyDao()
            dao.insert(pts)
            dao.prune(today - 400)
        }
    }

    /** Guarda la media nacional del día. [stations] debe ser la lista completa del país. */
    suspend fun recordNational(context: Context, fuelId: Int, stations: List<PricedStation>) {
        runCatching {
            val avg = nationalAverage(stations) ?: return
            val today = LocalDate.now().toEpochDay()
            AppDatabase.get(context).historyDao().insert(listOf(PricePoint(today, fuelId, NATIONAL_AVG, avg.average)))
        }
    }
}
