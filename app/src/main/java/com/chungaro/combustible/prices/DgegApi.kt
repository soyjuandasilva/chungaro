package com.chungaro.combustible.prices

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import com.chungaro.combustible.nearby.PricedStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Station(
    val id: Long,
    val name: String,
    val brand: String,
    val municipality: String,
    val locality: String,
    val address: String,
    /** Nombre del distrito si la API lo informa; vacío si no. */
    val district: String = "",
)

data class FuelPrice(
    val fuel: String,
    /** €/litro, o null si la gasolinera no informa precio. */
    val price: Double?,
    val updated: String,
)

data class StationDetail(
    val id: Long,
    val name: String,
    val brand: String,
    val address: String,
    val fuels: List<FuelPrice>,
)

/**
 * Cliente de la API pública de precios de combustibles de la DGEG
 * (Direção-Geral de Energia e Geologia, Portugal):
 * https://precoscombustiveis.dgeg.gov.pt
 */
object DgegApi {
    private const val BASE = "https://precoscombustiveis.dgeg.gov.pt/api/PrecoComb"

    val districts: List<Pair<Int, String>> = listOf(
        1 to "Aveiro", 2 to "Beja", 3 to "Braga", 4 to "Bragança",
        5 to "Castelo Branco", 6 to "Coimbra", 7 to "Évora", 8 to "Faro",
        9 to "Guarda", 10 to "Leiria", 11 to "Lisboa", 12 to "Portalegre",
        13 to "Porto", 14 to "Santarém", 15 to "Setúbal", 16 to "Viana do Castelo",
        17 to "Vila Real", 18 to "Viseu",
    )

    suspend fun stations(districtId: Int): List<Station> = withContext(Dispatchers.IO) {
        val all = parseStations(get("$BASE/PesquisarPostos?idDistrito=$districtId&qtdPorPagina=99999&pagina=1"))
        val name = districts.firstOrNull { it.first == districtId }?.second
        filterByDistrict(all, name)
            .sortedWith(compareBy({ normalize(it.municipality) }, { normalize(it.locality) }, { normalize(it.name) }))
    }

    /**
     * Si las filas traen el distrito, se queda solo con las del distrito pedido
     * (por si la API ignora el filtro). Si no traen distrito, no filtra.
     */
    fun filterByDistrict(stations: List<Station>, districtName: String?): List<Station> {
        if (districtName == null || stations.none { it.district.isNotBlank() }) return stations
        val target = normalize(districtName)
        val filtered = stations.filter { it.district.isBlank() || normalize(it.district) == target }
        return filtered.ifEmpty { stations }
    }

    fun normalize(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()

    /**
     * Todas las gasolineras de Portugal que venden [fuelTypeId], con su precio
     * y coordenadas. La API pagina de 500 en 500; "Quantidade" indica el total.
     */
    suspend fun pricedStations(fuelTypeId: Int): List<PricedStation> = withContext(Dispatchers.IO) {
        val all = ArrayList<PricedStation>()
        var page = 1
        var total = Int.MAX_VALUE
        while ((page - 1) * PAGE_SIZE < total && page <= MAX_PAGES) {
            val (rows, quantity) = parsePricedStations(
                get("$BASE/PesquisarPostos?idsTiposComb=$fuelTypeId&qtdPorPagina=$PAGE_SIZE&pagina=$page"),
            )
            all += rows.stations
            if (quantity != null) total = quantity
            if (rows.rawCount < PAGE_SIZE) break
            page++
        }
        all.distinctBy { it.id }
    }

    private const val PAGE_SIZE = 500
    private const val MAX_PAGES = 20

    data class PricedPage(val stations: List<PricedStation>, val rawCount: Int)

    /** Devuelve las filas válidas (con precio y coordenadas en Portugal) y el total informado. */
    fun parsePricedStations(json: String): Pair<PricedPage, Int?> {
        val arr = JSONObject(json).optJSONArray("resultado") ?: JSONArray()
        var quantity: Int? = null
        val list = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            if (quantity == null && o.has("Quantidade")) quantity = o.optInt("Quantidade", 0).takeIf { it > 0 }
            val id = o.optLong("Id", -1L).takeIf { it > 0 } ?: return@mapNotNull null
            val lat = o.coord("Latitude") ?: return@mapNotNull null
            val lon = o.coord("Longitude") ?: return@mapNotNull null
            if (lat !in 32.0..43.0 || lon !in -32.0..-6.0) return@mapNotNull null
            val price = parsePrice(o.str("Preco"))?.takeIf { it > 0 } ?: return@mapNotNull null
            PricedStation(
                id = id,
                name = o.str("Nome"),
                brand = o.str("Marca"),
                address = o.str("Morada"),
                locality = o.str("Localidade").ifBlank { o.str("Municipio") },
                lat = lat,
                lon = lon,
                price = price,
                updated = o.str("DataAtualizacao"),
            )
        }
        return PricedPage(list, arr.length()) to quantity
    }

    private fun JSONObject.coord(key: String): Double? = when (val v = opt(key)) {
        is Number -> v.toDouble()
        is String -> v.trim().replace(',', '.').toDoubleOrNull()
        else -> null
    }?.takeIf { it != 0.0 && !it.isNaN() }

    suspend fun station(id: Long): StationDetail = withContext(Dispatchers.IO) {
        parseStation(id, get("$BASE/GetDadosPosto?id=$id"))
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Chungaro-Android/1.0")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // --- Parseo (público para poder probarlo) ---

    fun parseStations(json: String): List<Station> {
        val arr = JSONObject(json).optJSONArray("resultado") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optLong("Id", -1L).takeIf { it > 0 } ?: return@mapNotNull null
            Station(
                id = id,
                name = o.str("Nome"),
                brand = o.str("Marca"),
                municipality = o.str("Municipio"),
                locality = o.str("Localidade"),
                address = o.address(),
                district = o.str("Distrito"),
            )
        }
            // La API puede repetir la misma gasolinera (una fila por combustible).
            .distinctBy { it.id }
    }

    fun parseStation(id: Long, json: String): StationDetail {
        val o = JSONObject(json).optJSONObject("resultado")
            ?: throw IOException("Respuesta sin datos de la gasolinera")
        val fuelsArr = o.optJSONArray("Combustiveis") ?: JSONArray()
        val fuels = (0 until fuelsArr.length()).mapNotNull { i ->
            val f = fuelsArr.optJSONObject(i) ?: return@mapNotNull null
            FuelPrice(
                fuel = f.str("TipoCombustivel"),
                price = parsePrice(f.str("Preco")),
                updated = f.str("DataAtualizacao"),
            )
        }
        return StationDetail(id, o.str("Nome"), o.str("Marca"), o.address(), fuels)
    }

    private val priceRegex = Regex("""\d+(?:[.,]\d+)?""")

    /** "1,749 €/litro" -> 1.749 */
    fun parsePrice(text: String): Double? =
        priceRegex.find(text)?.value?.replace(',', '.')?.toDoubleOrNull()

    private fun JSONObject.str(key: String): String =
        if (isNull(key)) "" else optString(key, "").trim()

    /** "Morada" puede venir como texto o como objeto {Morada, Localidade, CodPostal}. */
    private fun JSONObject.address(): String {
        val m = opt("Morada")
        return if (m is JSONObject) {
            listOf(m.str("Morada"), m.str("Localidade"), m.str("CodPostal"))
                .filter { it.isNotBlank() }
                .joinToString(", ")
        } else {
            str("Morada")
        }
    }
}
