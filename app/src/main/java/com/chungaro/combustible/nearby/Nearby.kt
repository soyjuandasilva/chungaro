package com.chungaro.combustible.nearby

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Gasolinera con el precio de un combustible concreto y su posición. */
data class PricedStation(
    val id: Long,
    val name: String,
    val brand: String,
    val address: String,
    val locality: String,
    val lat: Double,
    val lon: Double,
    /** €/litro. */
    val price: Double,
    val updated: String,
)

data class StationAtDistance(val station: PricedStation, val km: Double)

data class NearbyResult(
    /** La más barata dentro del radio (a igual precio, la más cercana). */
    val cheapest: StationAtDistance?,
    /** La más cercana, sin importar el radio. */
    val nearest: StationAtDistance?,
    /** Las más baratas dentro del radio, ordenadas por precio. */
    val cheapestList: List<StationAtDistance>,
    val averageInRadius: Double?,
    val countInRadius: Int,
)

/** Tipos de combustible de la DGEG (id usado en idsTiposComb). [color] en ARGB. */
data class FuelType(val id: Int, val name: String, val short: String, val color: Long)

val FUEL_TYPES = listOf(
    FuelType(3201, "Gasolina simples 95", "Gasolina 95", 0xFF16A34A),
    FuelType(3205, "Gasolina especial 95", "Gasolina 95+", 0xFF0D9488),
    FuelType(3400, "Gasolina 98", "Gasolina 98", 0xFF7C3AED),
    FuelType(2101, "Gasóleo simples", "Gasóleo", 0xFFD97706),
    FuelType(2105, "Gasóleo especial", "Gasóleo+", 0xFFB45309),
    FuelType(1120, "GPL Auto", "GPL", 0xFF2563EB),
)
const val DEFAULT_FUEL_ID = 3201

/** Distancia en km entre dos puntos (fórmula del haversine). */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

fun computeNearby(
    stations: List<PricedStation>,
    lat: Double,
    lon: Double,
    radiusKm: Double,
    listSize: Int = 5,
): NearbyResult {
    val withDistance = stations.map { StationAtDistance(it, distanceKm(lat, lon, it.lat, it.lon)) }
    val inRadius = withDistance.filter { it.km <= radiusKm }
    val byPrice = inRadius.sortedWith(compareBy({ it.station.price }, { it.km }))
    return NearbyResult(
        cheapest = byPrice.firstOrNull(),
        nearest = withDistance.minByOrNull { it.km },
        cheapestList = byPrice.take(listSize),
        averageInRadius = if (inRadius.isEmpty()) null else inRadius.sumOf { it.station.price } / inRadius.size,
        countInRadius = inRadius.size,
    )
}
