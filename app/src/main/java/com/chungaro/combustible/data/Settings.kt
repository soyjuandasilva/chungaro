package com.chungaro.combustible.data

import android.content.Context

data class AppSettings(
    /** "L" o "gal". */
    val unit: String = "L",
    val currency: String = "€",
    /** "L100" (L/100 km) o "KML" (km/L). */
    val consumptionUnit: String = "L100",
    val alertsEnabled: Boolean = false,
    /** Avisar cuando el combustible del vehículo baje de este precio (€/L). */
    val alertThreshold: Double? = null,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        unit = prefs.getString("unit", "L") ?: "L",
        currency = prefs.getString("currency", "€") ?: "€",
        consumptionUnit = prefs.getString("consumption_unit", "L100") ?: "L100",
        alertsEnabled = prefs.getBoolean("alerts", false),
        alertThreshold = prefs.getFloat("alert_threshold", -1f).takeIf { it > 0 }?.toDouble(),
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("unit", s.unit)
            .putString("currency", s.currency)
            .putString("consumption_unit", s.consumptionUnit)
            .putBoolean("alerts", s.alertsEnabled)
            .putFloat("alert_threshold", (s.alertThreshold ?: -1.0).toFloat())
            .apply()
    }

    fun loadSelectedVehicle(): Long = prefs.getLong("vehicle", 1L)

    fun saveSelectedVehicle(id: Long) {
        prefs.edit().putLong("vehicle", id).apply()
    }

    /** Última ubicación usada en Inicio (para alertas en segundo plano). */
    fun loadLastLocation(): Pair<Double, Double>? {
        if (!prefs.contains("last_lat")) return null
        return prefs.getFloat("last_lat", 0f).toDouble() to prefs.getFloat("last_lon", 0f).toDouble()
    }

    fun saveLastLocation(lat: Double, lon: Double) {
        prefs.edit().putFloat("last_lat", lat.toFloat()).putFloat("last_lon", lon.toFloat()).apply()
    }

    /** Clave del último aviso enviado, para no repetirlo. */
    fun lastNotified(key: String): String? = prefs.getString("notified_$key", null)

    fun setLastNotified(key: String, value: String) {
        prefs.edit().putString("notified_$key", value).apply()
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

    fun loadRadius(): Int = prefs.getInt("nearby_radius", 10)

    fun saveRadius(km: Int) {
        prefs.edit().putInt("nearby_radius", km).apply()
    }
}
