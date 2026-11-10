package com.chungaro.combustible.nearby

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Horario de una gasolinera desde OpenStreetMap (etiqueta opening_hours), vía la
 * API pública Overpass. Google Business Profile no tiene una API pública gratuita
 * para esto; OSM sí, aunque no todas las bombas tienen el horario puesto.
 */
object OpeningHours {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    /** opening_hours de la gasolinera de OSM más cercana a (lat, lon) en [radiusM] metros; null si no hay. */
    suspend fun lookup(lat: Double, lon: Double, radiusM: Int = 120): String? = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:15];nwr(around:$radiusM,$lat,$lon)[amenity=fuel];out tags center;"
        val conn = URL(ENDPOINT + "?data=" + URLEncoder.encode(q, "UTF-8")).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Atesta-Android/1.0")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
            parse(conn.inputStream.bufferedReader().use { it.readText() }, lat, lon)
        } finally {
            conn.disconnect()
        }
    }

    // --- Parseo y formato (públicos para poder probarlos) ---

    /** De la respuesta de Overpass, el opening_hours de la gasolinera más cercana que lo tenga. */
    fun parse(json: String, lat: Double, lon: Double): String? {
        val arr = JSONObject(json).optJSONArray("elements") ?: return null
        return (0 until arr.length()).mapNotNull { i ->
            val e = arr.optJSONObject(i) ?: return@mapNotNull null
            val hours = e.optJSONObject("tags")?.optString("opening_hours").orEmpty().trim()
            if (hours.isEmpty()) return@mapNotNull null
            val c = e.optJSONObject("center")
            val eLat = if (e.has("lat")) e.optDouble("lat") else c?.optDouble("lat") ?: return@mapNotNull null
            val eLon = if (e.has("lon")) e.optDouble("lon") else c?.optDouble("lon") ?: return@mapNotNull null
            distanceKm(lat, lon, eLat, eLon) to hours
        }.minByOrNull { it.first }?.second
    }

    private val days = mapOf("Mo" to "Seg", "Tu" to "Ter", "We" to "Qua", "Th" to "Qui", "Fr" to "Sex", "Sa" to "Sáb", "Su" to "Dom", "PH" to "Feriados")

    /**
     * Pasa el formato de OSM a líneas legibles en português:
     * "Mo-Fr 07:00-22:00; Sa,Su 08:00-20:00" → ["Seg–Sex: 07:00–22:00", "Sáb, Dom: 08:00–20:00"].
     */
    fun format(osm: String): List<String> {
        val s = osm.trim()
        if (s == "24/7") return listOf("Aberto 24 horas, todos os dias")
        return s.split(';').map { it.trim() }.filter { it.isNotEmpty() }.map { rule ->
            val m = Regex("""^([A-Za-z,\-\s]+?)\s+(.+)$""").find(rule)
            val (dayPart, timePart) = if (m != null && m.groupValues[1].split(Regex("[,\\-\\s]+")).all { it in days }) {
                m.groupValues[1] to m.groupValues[2]
            } else {
                "" to rule
            }
            val d = dayPart.replace(" ", "")
                .split(',')
                .joinToString(", ") { range -> range.split('-').joinToString("–") { days[it] ?: it } }
            val t = when (timePart.trim()) {
                "off", "closed" -> "Fechado"
                "00:00-24:00" -> "24 horas"
                else -> timePart.trim().replace("-", "–").replace(",", ", ")
            }
            if (d.isEmpty()) t else "$d: $t"
        }
    }
}
