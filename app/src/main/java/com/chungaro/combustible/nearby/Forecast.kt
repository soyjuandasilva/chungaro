package com.chungaro.combustible.nearby

import com.chungaro.combustible.data.PricePoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Precio medio actual de un combustible en todo el país. */
data class NationalPrice(val average: Double, val stations: Int)

/** Promedio de todas las gasolineras con precio (el universo completo descargado de la DGEG). */
fun nationalAverage(stations: List<PricedStation>): NationalPrice? {
    if (stations.isEmpty()) return null
    return NationalPrice(stations.sumOf { it.price } / stations.size, stations.size)
}

/** Estimación del precio medio para la semana que viene. */
data class Forecast(
    val price: Double,
    /** Variación estimada frente al precio actual, en %. */
    val changePct: Double,
)

/** Días que hacen falta en el histórico para estimar. */
const val FORECAST_MIN_SPAN_DAYS = 6
private const val FORECAST_WINDOW_DAYS = 14
/** La tendencia pasada no se traslada entera: se amortigua. */
private const val FORECAST_DAMPING = 0.6
/** Tope de la variación estimada (±%). */
private const val FORECAST_MAX_PCT = 4.0

/**
 * Estima el precio medio de la próxima semana a partir de la tendencia
 * (recta de mínimos cuadrados) de los últimos 14 días de [series], aplicada
 * a [current]. Devuelve null si el histórico cubre menos de ~1 semana.
 */
fun forecastNextWeek(series: List<PricePoint>, current: Double, horizonDays: Int = 7): Forecast? {
    if (current <= 0) return null
    val sorted = series.sortedBy { it.day }
    val lastDay = sorted.lastOrNull()?.day ?: return null
    val window = sorted.filter { it.day >= lastDay - FORECAST_WINDOW_DAYS && it.price > 0 }
    if (window.size < 4 || window.last().day - window.first().day < FORECAST_MIN_SPAN_DAYS) return null

    val n = window.size.toDouble()
    val meanX = window.sumOf { it.day.toDouble() } / n
    val meanY = window.sumOf { it.price } / n
    val sxx = window.sumOf { (it.day - meanX) * (it.day - meanX) }
    if (sxx == 0.0) return null
    val slope = window.sumOf { (it.day - meanX) * (it.price - meanY) } / sxx

    val pct = (slope * horizonDays / meanY * 100 * FORECAST_DAMPING).coerceIn(-FORECAST_MAX_PCT, FORECAST_MAX_PCT)
    return Forecast(current * (1 + pct / 100), pct)
}

/** En Portugal los precios suelen cambiar los lunes: el próximo lunes (nunca hoy). */
fun nextMonday(today: LocalDate = LocalDate.now()): LocalDate =
    today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
