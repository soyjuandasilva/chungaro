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
