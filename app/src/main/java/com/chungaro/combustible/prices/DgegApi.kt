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
    suspend fun pricedStations(fuelTypeId: Int, districtId: Int? = null): List<PricedStation> = withContext(Dispatchers.IO) {
        val all = ArrayList<PricedStation>()
        var page = 1
        var total = Int.MAX_VALUE
        val district = districtId?.let { "&idDistrito=$it" } ?: ""
        while ((page - 1) * PAGE_SIZE < total && page <= MAX_PAGES) {
            val (rows, quantity) = parsePricedStations(
                get("$BASE/PesquisarPostos?idsTiposComb=$fuelTypeId$district&qtdPorPagina=$PAGE_SIZE&pagina=$page"),
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

    /** Capitales de distrito (lat, lon), para saber en qué distritos buscar. */
    private val districtCenters: Map<Int, Pair<Double, Double>> = mapOf(
        1 to (40.6405 to -8.6538), 2 to (38.0151 to -7.8632), 3 to (41.5454 to -8.4265), 4 to (41.8061 to -6.7567),
        5 to (39.8222 to -7.4909), 6 to (40.2033 to -8.4103), 7 to (38.5714 to -7.9135), 8 to (37.0194 to -7.9322),
        9 to (40.5373 to -7.2658), 10 to (39.7436 to -8.8071), 11 to (38.7223 to -9.1393), 12 to (39.2967 to -7.4285),
        13 to (41.1579 to -8.6291), 14 to (39.2362 to -8.6859), 15 to (38.5244 to -8.8882), 16 to (41.6932 to -8.8329),
        17 to (41.3006 to -7.7441), 18 to (40.6566 to -7.9125),
    )

    /** Los [count] distritos cuya capital está más cerca de un punto. */
    fun nearestDistricts(lat: Double, lon: Double, count: Int = 2): List<Int> =
        districtCenters.entries
            .sortedBy { (_, c) -> com.chungaro.combustible.nearby.distanceKm(lat, lon, c.first, c.second) }
            .take(count)
            .map { it.key }

    suspend fun station(id: Long): StationDetail = withContext(Dispatchers.IO) {
        parseStation(id, get("$BASE/GetDadosPosto?id=$id"))
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "AbasteceHoje-Android/1.0")
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
            ?: throw IOException("Resposta sem dados do posto")
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
