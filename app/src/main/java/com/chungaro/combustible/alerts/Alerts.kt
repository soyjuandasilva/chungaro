package com.chungaro.combustible.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chungaro.combustible.MainActivity
import com.chungaro.combustible.R
import com.chungaro.combustible.data.AppDatabase
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.PriceCache
import com.chungaro.combustible.nearby.computeNearby
import com.chungaro.combustible.prices.DgegApi
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit

object AlertScheduler {
    private const val WORK = "chungaro-alerts"

    /** Programa la revisión periódica (cada 6 h). Es idempotente. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AlertWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

object Notifier {
    const val CHANNEL_PRICES = "prices"
    /** Canal de las versiones con «O meu veículo»; se borra si existe. */
    private const val CHANNEL_MAINTENANCE = "maintenance"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PRICES, "Alertas de preço", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.deleteNotificationChannel(CHANNEL_MAINTENANCE)
    }

    fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            // Permiso retirado entre la comprobación y el aviso.
        }
    }
}

class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val store = SettingsStore(ctx)
        // El histórico de precios se guarda siempre; el aviso, solo con permiso.
        runCatching { checkPrice(ctx, store) }
        return Result.success()
    }

    /**
     * Guarda el histórico de precios de todos los combustibles y después avisa
     * si el combustible de la alerta bajó del precio-alvo.
     */
    private suspend fun checkPrice(ctx: Context, store: SettingsStore) {
        val settings = store.load()
        val (lat, lon) = store.loadLastLocation() ?: return
        // Histórico de TODOS los combustibles y cada bomba.
        // Para gastar pocos datos, solo los dos distritos más cercanos.
        val districts = DgegApi.nearestDistricts(lat, lon)
        val downloaded = mutableMapOf<Int, List<com.chungaro.combustible.nearby.PricedStation>>()
        FUEL_TYPES.forEach { f ->
            runCatching { districts.flatMap { d -> DgegApi.pricedStations(f.id, d) }.distinctBy { it.id } }
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        downloaded[f.id] = list
                        com.chungaro.combustible.nearby.HistoryRecorder.record(ctx, f.id, list, lat, lon)
                    }
                }
        }

        // Aviso de precio: combustible elegido en Alertas (o el habitual).
        val fuelId = settings.alertFuelId ?: settings.fuelId
        val fuel = FUEL_TYPES.firstOrNull { it.id == fuelId } ?: return
        val threshold = settings.alertThreshold ?: return
        if (!settings.alertsEnabled || !Notifier.canNotify(ctx)) return
        // Solo favoritas: se buscan en todo el país (pueden estar lejos de los distritos descargados).
        val stations = if (settings.alertFavoritesOnly) {
            val favs = store.loadFavorites().toSet()
            if (favs.isEmpty()) return
            runCatching { DgegApi.pricedStations(fuel.id) }.getOrNull()?.filter { it.id in favs } ?: return
        } else {
            downloaded[fuel.id] ?: return
        }
        val cheapest = if (settings.alertFavoritesOnly) {
            stations.minByOrNull { it.price }?.let { s ->
                com.chungaro.combustible.nearby.StationAtDistance(s, com.chungaro.combustible.nearby.distanceKm(lat, lon, s.lat, s.lon))
            } ?: return
        } else {
            computeNearby(stations, lat, lon, store.loadRadius().toDouble()).cheapest ?: return
        }
        if (cheapest.station.price > threshold) return
        val key = "price_${fuel.id}"
        val stamp = "${LocalDate.now()}_${cheapest.station.id}_${cheapest.station.price}"
        if (store.lastNotified(key) == stamp) return
        store.setLastNotified(key, stamp)
        val name = cheapest.station.name.ifBlank { cheapest.station.brand }
        Notifier.show(
            ctx,
            Notifier.CHANNEL_PRICES,
            fuel.id,
            "${fuel.short} a ${String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f €", cheapest.station.price)}",
            "$name, a ${String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.1f km", cheapest.km)}. " +
                "Está abaixo do teu alerta (${String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.3f €", threshold)}).",
        )
    }
}
