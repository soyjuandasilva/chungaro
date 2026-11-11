package com.chungaro.combustible.ui

import com.chungaro.combustible.data.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** A app está em português de Portugal: datas e meses neste idioma. */
val APP_LOCALE: Locale = Locale.forLanguageTag("pt-PT")

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", APP_LOCALE)

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

fun formatNumber(v: Double, decimals: Int = 2): String =
    String.format(com.chungaro.combustible.ui.APP_LOCALE, "%,.${decimals}f", v)

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
