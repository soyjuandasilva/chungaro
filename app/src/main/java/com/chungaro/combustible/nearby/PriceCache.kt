package com.chungaro.combustible.nearby

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Guarda en el teléfono los últimos precios descargados para mostrarlos al instante. */
object PriceCache {
    private fun file(context: Context, fuelId: Int) = File(context.cacheDir, "prices_$fuelId.json")

    fun save(context: Context, fuelId: Int, stations: List<PricedStation>) {
        runCatching {
            val arr = JSONArray()
            stations.forEach { s ->
                arr.put(
                    JSONObject().put("id", s.id).put("name", s.name).put("brand", s.brand).put("address", s.address)
                        .put("locality", s.locality).put("lat", s.lat).put("lon", s.lon).put("price", s.price)
                        .put("updated", s.updated),
                )
            }
            file(context, fuelId).writeText(JSONObject().put("savedAt", System.currentTimeMillis()).put("stations", arr).toString())
        }
    }

    /** (momento de guardado, gasolineras) o null si no hay caché. */
    fun load(context: Context, fuelId: Int): Pair<Long, List<PricedStation>>? = runCatching {
        val f = file(context, fuelId)
        if (!f.exists()) return null
        val root = JSONObject(f.readText())
        val arr = root.getJSONArray("stations")
        val list = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PricedStation(
                id = o.getLong("id"),
                name = o.optString("name"),
                brand = o.optString("brand"),
                address = o.optString("address"),
                locality = o.optString("locality"),
                lat = o.getDouble("lat"),
                lon = o.getDouble("lon"),
                price = o.getDouble("price"),
                updated = o.optString("updated"),
            )
        }
        root.getLong("savedAt") to list
    }.getOrNull()
}
