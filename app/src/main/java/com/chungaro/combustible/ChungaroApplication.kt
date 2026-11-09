package com.chungaro.combustible

import android.app.Application
import android.content.Context
import java.io.File

/** Guarda el último cierre inesperado para mostrarlo en el siguiente arranque. */
class ChungaroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.chungaro.combustible.alerts.Notifier.ensureChannels(this)
        com.chungaro.combustible.alerts.AlertScheduler.schedule(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                crashFile(this).writeText(
                    "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE} · " +
                        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
                        error.stackTraceToString(),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        private fun crashFile(context: Context) = File(context.filesDir, "last_crash.txt")

        /** Devuelve el informe del último cierre (y lo borra), o null si no hubo. */
        fun takeLastCrash(context: Context): String? {
            val f = crashFile(context)
            if (!f.exists()) return null
            return runCatching { f.readText() }.getOrNull().also { f.delete() }
        }
    }
}
