package com.chungaro.combustible.data

import android.content.Context

data class AppSettings(
    /** "L" o "gal". */
    val unit: String = "L",
    val currency: String = "€",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        unit = prefs.getString("unit", "L") ?: "L",
        currency = prefs.getString("currency", "€") ?: "€",
    )

    fun save(s: AppSettings) {
        prefs.edit().putString("unit", s.unit).putString("currency", s.currency).apply()
    }

    /** Ids de gasolineras favoritas (DGEG), en el orden en que se agregaron. */
    fun loadFavorites(): List<Long> =
        prefs.getString("favorites", "").orEmpty()
            .split(',')
            .mapNotNull { it.trim().toLongOrNull() }

    fun saveFavorites(ids: List<Long>) {
        prefs.edit().putString("favorites", ids.joinToString(",")).apply()
    }

    fun loadDistrict(): Int? = prefs.getInt("district", 0).takeIf { it > 0 }

    fun saveDistrict(id: Int) {
        prefs.edit().putInt("district", id).apply()
    }

    fun loadNearbyFuel(): Int = prefs.getInt("nearby_fuel", com.chungaro.combustible.nearby.DEFAULT_FUEL_ID)

    fun saveNearbyFuel(id: Int) {
        prefs.edit().putInt("nearby_fuel", id).apply()
    }

    fun loadRadius(): Int = prefs.getInt("nearby_radius", 10)

    fun saveRadius(km: Int) {
        prefs.edit().putInt("nearby_radius", km).apply()
    }
}
