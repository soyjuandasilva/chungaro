package com.chungaro.combustible.nearby

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LISBON: ZoneId = ZoneId.of("Europe/Lisbon")

private val dgegFormats = listOf(
    "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm",
    "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm", "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm",
).map { DateTimeFormatter.ofPattern(it) }

/** Fecha de actualización de la DGEG (hora de Lisboa) en milisegundos, o null si no se reconoce. */
fun parseDgegDate(text: String): Long? {
    val s = text.trim().substringBefore('.')
    if (s.isEmpty()) return null
    for (f in dgegFormats) {
        val t = runCatching { LocalDateTime.parse(s, f) }.getOrNull() ?: continue
        return t.atZone(LISBON).toInstant().toEpochMilli()
    }
    return null
}

/** "agora", "há 25 min", "há 10 h", "há 3 dias". */
fun agoText(time: Long, now: Long = System.currentTimeMillis()): String {
    val min = ((now - time) / 60_000L).coerceAtLeast(0)
    return when {
        min < 1 -> "agora"
        min < 60 -> "há $min min"
        min < 24 * 60 -> "há ${min / 60} h"
        min < 48 * 60 -> "há 1 dia"
        else -> "há ${min / (24 * 60)} dias"
    }
}

/** Texto corto de cuándo se actualizó un precio: "há 10 h", o la fecha tal cual si no se entiende. */
fun updatedText(raw: String, now: Long = System.currentTimeMillis()): String =
    parseDgegDate(raw)?.let { agoText(it, now) } ?: raw
