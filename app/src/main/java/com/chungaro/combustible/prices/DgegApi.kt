package com.chungaro.combustible.prices

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
        parseStations(get("$BASE/PesquisarPostos?idDistrito=$districtId&qtdPorPagina=99999&pagina=1"))
    }

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
            )
        }
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
