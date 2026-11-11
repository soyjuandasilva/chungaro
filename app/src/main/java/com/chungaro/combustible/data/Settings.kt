package com.chungaro.combustible.data

import android.content.Context

data class AppSettings(
    /** Combustible habitual (id de la DGEG): el que se muestra por defecto en toda la app. */
    val fuelId: Int = DEFAULT_FUEL,
    val alertsEnabled: Boolean = false,
    /** Avisar cuando el combustible baje de este precio (€/L). */
    val alertThreshold: Double? = null,
    /** Combustible del aviso; null = el combustible habitual. */
    val alertFuelId: Int? = null,
    /** Avisar solo por las bombas favoritas (si no, todas las de la zona). */
    val alertFavoritesOnly: Boolean = false,
    /** Tema: "auto" (el del sistema), "light" o "dark". */
    val themeMode: String = "auto",
) {
    companion object {
        /** Gasolina simples 95. */
        const val DEFAULT_FUEL = 3201
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        fuelId = prefs.getInt("fuel", AppSettings.DEFAULT_FUEL),
        alertsEnabled = prefs.getBoolean("alerts", false),
        alertThreshold = prefs.getFloat("alert_threshold", -1f).takeIf { it > 0 }?.toDouble(),
        alertFuelId = prefs.getInt("alert_fuel", 0).takeIf { it > 0 },
        alertFavoritesOnly = prefs.getBoolean("alert_favorites_only", false),
        themeMode = prefs.getString("theme_mode", "auto") ?: "auto",
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putInt("fuel", s.fuelId)
            .putBoolean("alerts", s.alertsEnabled)
            .putFloat("alert_threshold", (s.alertThreshold ?: -1.0).toFloat())
            .putInt("alert_fuel", s.alertFuelId ?: 0)
            .putBoolean("alert_favorites_only", s.alertFavoritesOnly)
            .putString("theme_mode", s.themeMode)
            .apply()
    }

    /** Si ya hay combustible habitual guardado. */
    fun hasFuel(): Boolean = prefs.contains("fuel")

    fun saveFuel(id: Int) {
        prefs.edit().putInt("fuel", id).apply()
    }

    /** Vehículo elegido en versiones antiguas (solo para heredar su combustible). */
    fun legacySelectedVehicle(): Long = prefs.getLong("vehicle", 1L)

    /** Borra los ajustes que solo usaba «O meu veículo». */
    fun clearLegacyVehicleSettings() {
        prefs.edit().remove("vehicle").remove("unit").remove("currency").remove("consumption_unit").apply()
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

    /** Si ya se vio el tour de bienvenida. */
    fun tourSeen(): Boolean = prefs.getBoolean("tour_seen", false)

    fun setTourSeen() {
        prefs.edit().putBoolean("tour_seen", true).apply()
    }
}
