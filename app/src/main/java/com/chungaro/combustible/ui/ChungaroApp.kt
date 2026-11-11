package com.chungaro.combustible.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chungaro.combustible.ChungaroApplication
import com.chungaro.combustible.alerts.Notifier
import com.chungaro.combustible.data.SettingsStore
import com.chungaro.combustible.nearby.AlertsScreen
import com.chungaro.combustible.nearby.MainHome
import com.chungaro.combustible.nearby.NearbyViewModel
import com.chungaro.combustible.nearby.SavingsCalculatorScreen
import com.chungaro.combustible.nearby.StationSheet
import com.chungaro.combustible.nearby.TANK_LITERS
import com.chungaro.combustible.prices.PricesScreen
import com.chungaro.combustible.prices.PricesViewModel

/** Pantallas de la app. Início es la base; las demás se abren encima. */
private const val SCREEN_HOME = 0
private const val SCREEN_SETTINGS = 1
private const val SCREEN_ALERTS = 2
private const val SCREEN_CALCULATOR = 3
/** Procurar bombas por distrito. */
private const val SCREEN_DISTRICT = 4

@Composable
fun ChungaroApp(
    vm: AppViewModel = viewModel(),
    pricesVm: PricesViewModel = viewModel(),
    nearbyVm: NearbyViewModel = viewModel(),
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    var screen by rememberSaveable { mutableIntStateOf(SCREEN_HOME) }
    var crashReport by rememberSaveable { mutableStateOf(ChungaroApplication.takeLastCrash(context)) }
    // Tour de bienvenida: la primera vez que se abre la app (y desde Definições).
    var showTour by rememberSaveable { mutableStateOf(!store.tourSeen()) }
    val fuelId = vm.settings.fuelId

    // Permiso de notificaciones para los avisos de precio (Android 13+).
    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted
        if (granted) vm.updateSettings(vm.settings.copy(alertsEnabled = true))
    }
    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else canNotify = true
    }

    if (showTour) {
        QuickTour(onFinish = {
            store.setTourSeen()
            showTour = false
            screen = SCREEN_HOME
        })
        return
    }

    val reduceMotion = rememberReduceMotion()
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            // Hacia dentro (desde Início) entra por la derecha; al volver, al revés.
            screenTransition(forward = targetState != SCREEN_HOME, reduce = reduceMotion)
        },
        label = "screen",
    ) { current ->
        when (current) {
            SCREEN_SETTINGS -> {
                BackHandler { screen = SCREEN_HOME }
                SettingsScreen(
                    settings = vm.settings,
                    onChange = { vm.updateSettings(it) },
                    onBack = { screen = SCREEN_HOME },
                    onAlerts = { screen = SCREEN_ALERTS },
                    onCalculator = { screen = SCREEN_CALCULATOR },
                    onTour = { screen = SCREEN_HOME; showTour = true },
                )
            }
            SCREEN_CALCULATOR -> {
                BackHandler { screen = SCREEN_HOME }
                var calcStation by rememberSaveable { mutableStateOf<Long?>(null) }
                SavingsCalculatorScreen(
                    vm = nearbyVm,
                    fuelId = fuelId,
                    tankLiters = TANK_LITERS.toDouble(),
                    onBack = { screen = SCREEN_HOME },
                    onOpenStation = { calcStation = it },
                )
                calcStation?.let { sid ->
                    StationSheet(nearbyVm, sid, fuelId, onDismiss = { calcStation = null }, isFavorite = sid in pricesVm.favorites, onToggleFavorite = { pricesVm.toggleFavoriteId(sid) })
                }
            }
            SCREEN_ALERTS -> {
                BackHandler { screen = SCREEN_HOME }
                AlertsScreen(
                    settings = vm.settings,
                    defaultFuelId = fuelId,
                    favoritesCount = pricesVm.favorites.size,
                    canNotify = canNotify,
                    onRequestNotifications = requestNotifications,
                    onSave = { vm.updateSettings(it) },
                    onBack = { screen = SCREEN_HOME },
                )
            }
            SCREEN_DISTRICT -> {
                BackHandler { screen = SCREEN_HOME }
                PricesScreen(pricesVm, onBack = { screen = SCREEN_HOME })
            }
            else -> MainHome(
                vm = nearbyVm,
                myFuelId = fuelId,
                tankLiters = TANK_LITERS.toDouble(),
                favorites = pricesVm.favorites,
                onToggleFavorite = { pricesVm.toggleFavoriteId(it) },
                alertsEnabled = vm.settings.alertsEnabled,
                onToggleAlerts = { on ->
                    if (on && !canNotify) requestNotifications() else vm.updateSettings(vm.settings.copy(alertsEnabled = on))
                },
                onAlerts = { screen = SCREEN_ALERTS },
                onSettings = { screen = SCREEN_SETTINGS },
                onCalculator = { screen = SCREEN_CALCULATOR },
                onSearchByDistrict = { screen = SCREEN_DISTRICT },
            )
        }
    }

    crashReport?.let { report ->
        CrashDialog(report, onDismiss = { crashReport = null })
    }
}
