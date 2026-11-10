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
    const val CHANNEL_MAINTENANCE = "maintenance"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PRICES, "Alertas de preço", NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MAINTENANCE, "Manutenção", NotificationManager.IMPORTANCE_DEFAULT),
        )
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
            .setSmallIcon(R.drawable.ic_fuel)
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
        val dao = AppDatabase.get(ctx).refuelDao()
        // El histórico de precios se guarda siempre; los avisos, solo con permiso.
        if (Notifier.canNotify(ctx)) checkReminders(ctx, store, dao)
        runCatching { checkPrice(ctx, store, dao) }
        return Result.success()
    }

    private suspend fun checkReminders(
        ctx: Context,
        store: SettingsStore,
        dao: com.chungaro.combustible.data.RefuelDao,
    ) {
        val today = LocalDate.now().toEpochDay()
        val vehicles = dao.vehicles().associateBy { it.id }
        dao.pendingReminders().forEach { r ->
            val odo = dao.maxOdometer(r.vehicleId)
            val byDate = r.dueEpochDay?.let { it - today <= 7 } ?: false
            val byKm = if (r.dueOdometer != null && odo != null) r.dueOdometer - odo <= 500 else false
            if (!byDate && !byKm) return@forEach
            val key = "rem_${r.id}"
            val stamp = today.toString()
            if (store.lastNotified(key) == stamp) return@forEach
            store.setLastNotified(key, stamp)
            val vehicle = vehicles[r.vehicleId]?.name ?: ""
            val detail = reminderStatus(r.dueEpochDay, r.dueOdometer, today, odo)
            Notifier.show(ctx, Notifier.CHANNEL_MAINTENANCE, 1000 + r.id.toInt(), "${r.title} · $vehicle", detail)
        }
    }

    /**
     * Guarda el histórico de precios automáticamente: cada vez, los combustibles
     * de todos los vehículos; una vez al día, todos los combustibles. Después
     * avisa si el combustible del vehículo en uso bajó del umbral.
     */
    private suspend fun checkPrice(ctx: Context, store: SettingsStore, dao: com.chungaro.combustible.data.RefuelDao) {
        val settings = store.load()
        val (lat, lon) = store.loadLastLocation() ?: return
        val vehicles = dao.vehicles()
        // Histórico de TODOS los combustibles y cada bomba, independiente del vehículo.
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

        // Aviso de precio para el vehículo en uso.
        val vehicle = vehicles.firstOrNull { it.id == store.loadSelectedVehicle() } ?: vehicles.firstOrNull() ?: return
        val fuel = FUEL_TYPES.firstOrNull { it.id == vehicle.fuelTypeId } ?: return
        val stations = downloaded[fuel.id] ?: return
        val threshold = settings.alertThreshold ?: return
        if (!settings.alertsEnabled || !Notifier.canNotify(ctx)) return
        val cheapest = computeNearby(stations, lat, lon, store.loadRadius().toDouble()).cheapest ?: return
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
            "${fuel.short} a ${String.format(Locale.getDefault(), "%.3f €", cheapest.station.price)}",
            "$name, a ${String.format(Locale.getDefault(), "%.1f km", cheapest.km)}. " +
                "Está abaixo do seu alerta (${String.format(Locale.getDefault(), "%.3f €", threshold)}).",
        )
    }
}

/** Texto del estado de un recordatorio (también usado en la pantalla). */
fun reminderStatus(dueEpochDay: Long?, dueOdometer: Double?, today: Long, odometer: Double?): String {
    val parts = mutableListOf<String>()
    dueEpochDay?.let {
        val days = it - today
        parts += when {
            days < 0 -> "venceu há ${-days} dia(s)"
            days == 0L -> "vence hoje"
            else -> "vence daqui a $days dia(s)"
        }
    }
    if (dueOdometer != null) {
        if (odometer != null) {
            val left = dueOdometer - odometer
            parts += if (left <= 0) "ultrapassado em ${String.format(Locale.getDefault(), "%,.0f", -left)} km"
            else "faltam ${String.format(Locale.getDefault(), "%,.0f", left)} km"
        } else {
            parts += "aos ${String.format(Locale.getDefault(), "%,.0f", dueOdometer)} km"
        }
    }
    return parts.joinToString(" · ").replaceFirstChar { it.uppercase() }
}
