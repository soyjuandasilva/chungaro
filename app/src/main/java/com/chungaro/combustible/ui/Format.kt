package com.chungaro.combustible.ui

import com.chungaro.combustible.data.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

fun formatNumber(v: Double, decimals: Int = 2): String =
    String.format(Locale.getDefault(), "%,.${decimals}f", v)

fun AppSettings.money(v: Double): String = currency + formatNumber(v)

/** Con 3 decimales, para precios por litro/galón. */
fun AppSettings.moneyPrecise(v: Double): String = currency + formatNumber(v, 3)

/** Consumo a partir del rendimiento (km por unidad), en la unidad elegida. */
fun AppSettings.consumption(kmPerUnit: Double): String =
    if (consumptionUnit == "L100") "${formatNumber(100.0 / kmPerUnit, 1)} $unitLabel/100 km"
    else "${formatNumber(kmPerUnit)} km/$unitLabel"

/** Valor numérico del consumo en la unidad elegida (para gráficos). */
fun AppSettings.consumptionValue(kmPerUnit: Double): Double =
    if (consumptionUnit == "L100") 100.0 / kmPerUnit else kmPerUnit

val AppSettings.consumptionLabel: String
    get() = if (consumptionUnit == "L100") "$unitLabel/100 km" else "km/$unitLabel"

/** Nombre corto de la unidad: "L" o "gal". */
val AppSettings.unitLabel: String get() = if (unit == "gal") "gal" else "L"

/** Convierte texto del usuario a número; acepta coma o punto decimal. */
fun parseNumber(s: String): Double? =
    s.trim().replace(" ", "").replace(',', '.').toDoubleOrNull()

/** Número para precargar un campo editable (sin separadores de miles). */
fun editableNumber(v: Double, decimals: Int = 3): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    val rounded = (v * factor).roundToLong() / factor
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
    else rounded.toBigDecimal().stripTrailingZeros().toPlainString()
}
