package com.chungaro.combustible.nearby

/** Precio medio actual de un combustible en todo el país. */
data class NationalPrice(val average: Double, val stations: Int)

/** Promedio de todas las gasolineras con precio (el universo completo descargado de la DGEG). */
fun nationalAverage(stations: List<PricedStation>): NationalPrice? {
    if (stations.isEmpty()) return null
    return NationalPrice(stations.sumOf { it.price } / stations.size, stations.size)
}
