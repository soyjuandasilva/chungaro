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
    /** Servidor principal y espejos públicos de Overpass, por si uno falla o está saturado. */
    private val ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )

    /**
     * opening_hours de la gasolinera de OSM más cercana a (lat, lon) en [radiusM] metros;
     * null si no está en OSM o no tiene horario. Lanza excepción si ningún servidor responde.
     */
    suspend fun lookup(lat: Double, lon: Double, radiusM: Int = 250): String? = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:20];nwr(around:$radiusM,$lat,$lon)[amenity=fuel];out tags center;"
        var last: Exception? = null
        for (endpoint in ENDPOINTS) {
            try {
                return@withContext parse(post(endpoint, q), lat, lon)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("Overpass sem resposta")
    }

    private fun post(endpoint: String, query: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 25_000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", "Atesta-Android/1.0 (app de precos de combustiveis)")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
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
