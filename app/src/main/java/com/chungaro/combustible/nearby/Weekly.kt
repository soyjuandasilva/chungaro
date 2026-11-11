package com.chungaro.combustible.nearby

import com.chungaro.combustible.data.PricePoint

/** Cambio de precio de una bomba entre hace ~1 semana y ahora. */
data class StationChange(val stationId: Long, val before: Double, val now: Double) {
    val diff: Double get() = now - before
}

enum class WeeklyAdvice { COLLECTING, FILL_THIS_WEEK, WAIT, STABLE }

data class WeeklyInsight(
    val advice: WeeklyAdvice,
    /** Variación de la media de la zona frente a hace ~7 días, en %. */
    val changePct: Double?,
    /** Días con datos guardados. */
    val days: Int,
    val zoneNow: Double?,
    val zoneBefore: Double?,
    /** Bombas que bajaron más (de mayor a menor bajada). */
    val dropped: List<StationChange>,
    /** Bombas que subieron más. */
    val risen: List<StationChange>,
)

const val WEEK_DAYS = 7
private const val THRESHOLD_PCT = 0.8

/**
 * Compara la semana actual con la anterior usando el histórico guardado:
 * media de la zona (stationId = ZONE_AVG) y cada bomba.
 */
fun computeWeekly(zone: List<PricePoint>, stations: List<PricePoint>, top: Int = 3): WeeklyInsight {
    val z = zone.sortedBy { it.day }
    val days = z.map { it.day }.distinct().size
    val last = z.lastOrNull() ?: return WeeklyInsight(WeeklyAdvice.COLLECTING, null, 0, null, null, emptyList(), emptyList())
    val ref = z.lastOrNull { it.day <= last.day - WEEK_DAYS }
    if (ref == null) {
        return WeeklyInsight(WeeklyAdvice.COLLECTING, null, days, last.price, null, emptyList(), emptyList())
    }
    val pct = (last.price - ref.price) / ref.price * 100
    val advice = when {
        pct <= -THRESHOLD_PCT -> WeeklyAdvice.FILL_THIS_WEEK
        pct >= THRESHOLD_PCT -> WeeklyAdvice.WAIT
        else -> WeeklyAdvice.STABLE
    }
    // Por bomba: último precio frente al de hace ~1 semana.
    val changes = stations.groupBy { it.stationId }.mapNotNull { (id, pts) ->
        val sorted = pts.sortedBy { it.day }
        val now = sorted.last()
        if (now.day < last.day - 1) return@mapNotNull null // sin datos recientes
        val before = sorted.lastOrNull { it.day <= now.day - WEEK_DAYS } ?: return@mapNotNull null
        StationChange(id, before.price, now.price).takeIf { kotlin.math.abs(it.diff) >= 0.0005 }
    }
    return WeeklyInsight(
        advice = advice,
        changePct = pct,
        days = days,
        zoneNow = last.price,
        zoneBefore = ref.price,
        dropped = changes.filter { it.diff < 0 }.sortedBy { it.diff }.take(top),
        risen = changes.filter { it.diff > 0 }.sortedByDescending { it.diff }.take(top),
    )
}
