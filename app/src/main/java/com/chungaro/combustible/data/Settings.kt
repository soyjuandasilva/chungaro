package com.chungaro.combustible.data

import android.content.Context

data class AppSettings(
    /** "L" o "gal". */
    val unit: String = "L",
    val currency: String = "$",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = AppSettings(
        unit = prefs.getString("unit", "L") ?: "L",
        currency = prefs.getString("currency", "$") ?: "$",
    )

    fun save(s: AppSettings) {
        prefs.edit().putString("unit", s.unit).putString("currency", s.currency).apply()
    }
}
