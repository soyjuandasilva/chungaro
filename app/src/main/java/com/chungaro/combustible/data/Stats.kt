package com.chungaro.combustible.data

import java.time.LocalDate
import java.time.YearMonth

/** Un abastecimiento con los datos calculados respecto a los anteriores. */
data class RefuelRow(
    val refuel: Refuel,
    /** Km recorridos desde el abastecimiento anterior. */
    val distance: Double?,
    /** Km por unidad (L o gal) del tramo que termina en este llenado. */
    val efficiency: Double?,
    /** Costo por km del tramo que termina en este llenado. */
    val costPerKm: Double?,
)

data class Summary(
    val count: Int = 0,
    val totalCost: Double = 0.0,
    val totalQuantity: Double = 0.0,
    val totalDistance: Double = 0.0,
    val avgPrice: Double? = null,
    val avgEfficiency: Double? = null,
    val avgCostPerKm: Double? = null,
    val monthCost: Double = 0.0,
    val monthQuantity: Double = 0.0,
)

data class Stats(val rows: List<RefuelRow>, val summary: Summary)

/**
 * Calcula el rendimiento con el método de tanque lleno: entre dos llenados
 * completos, el combustible gastado es la suma de lo cargado después del
 * primero (incluidas cargas parciales) hasta el segundo inclusive.
 *
 * [refuels] debe venir ordenado por fecha/odómetro ascendente.
 * Las filas se devuelven en el mismo orden.
 */
fun computeStats(refuels: List<Refuel>, today: LocalDate = LocalDate.now()): Stats {
    val rows = ArrayList<RefuelRow>(refuels.size)
    var lastFull: Refuel? = null
    var segQty = 0.0
    var segCost = 0.0
    var sumSegDist = 0.0
    var sumSegQty = 0.0
    var sumSegCost = 0.0

    refuels.forEachIndexed { i, r ->
        val prev = refuels.getOrNull(i - 1)
        val distance = prev?.let { r.odometer - it.odometer }?.takeIf { it > 0 }

        var efficiency: Double? = null
        var costPerKm: Double? = null
        if (lastFull != null) {
            segQty += r.quantity
            segCost += r.totalCost
        }
        if (r.fullTank) {
            val base = lastFull
            if (base != null) {
                val segDist = r.odometer - base.odometer
                if (segDist > 0 && segQty > 0) {
                    efficiency = segDist / segQty
                    costPerKm = segCost / segDist
                    sumSegDist += segDist
                    sumSegQty += segQty
                    sumSegCost += segCost
                }
            }
            lastFull = r
            segQty = 0.0
            segCost = 0.0
        }
        rows += RefuelRow(r, distance, efficiency, costPerKm)
    }

    val month = YearMonth.from(today)
    val inMonth = refuels.filter { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month }
    val totalQty = refuels.sumOf { it.quantity }
    val totalCost = refuels.sumOf { it.totalCost }
    val totalDistance = if (refuels.size >= 2) {
        (refuels.maxOf { it.odometer } - refuels.minOf { it.odometer }).coerceAtLeast(0.0)
    } else 0.0

    val summary = Summary(
        count = refuels.size,
        totalCost = totalCost,
        totalQuantity = totalQty,
        totalDistance = totalDistance,
        avgPrice = if (totalQty > 0) totalCost / totalQty else null,
        avgEfficiency = if (sumSegQty > 0) sumSegDist / sumSegQty else null,
        avgCostPerKm = if (sumSegDist > 0) sumSegCost / sumSegDist else null,
        monthCost = inMonth.sumOf { it.totalCost },
        monthQuantity = inMonth.sumOf { it.quantity },
    )
    return Stats(rows, summary)
}
