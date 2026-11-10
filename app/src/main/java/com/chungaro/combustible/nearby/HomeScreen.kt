package com.chungaro.combustible.nearby

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import com.chungaro.combustible.ui.StatusBarIcons
import com.chungaro.combustible.ui.atestaTopBarColors
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.emoji
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chungaro.combustible.R
import java.text.DateFormat
import java.util.Date


@Composable
fun HomeScreen(
    vm: NearbyViewModel,
    myFuelId: Int,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(LocationHelper.hasPermission(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPermission = res.values.any { it }
        if (hasPermission) vm.refresh()
    }
    // Los precios medios del país no necesitan ubicación: se descargan siempre.
    LaunchedEffect(hasPermission) {
        if (!vm.started) vm.refresh()
    }
    var stationSheet by rememberSaveable { mutableStateOf<Long?>(null) }

    // Recarga sola: al volver a la app y cada 10 minutos mientras se ve Início.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && vm.started) vm.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10 * 60_000L)
            vm.refresh()
        }
    }

    Scaffold(topBar = { TopBar(vm, onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            // 1) Precio medio de hoy en todo el país, en grande
            NationalPricesCard(vm, myFuelId)

            // 2) Evolución de precios en tu zona (necesita la ubicación)
            SectionCard(
                icon = "📈",
                accent = SectionColors.Blue,
                title = "Evolução dos preços",
                subtitle = "Na tua zona, face à semana passada",
            ) {
                if (hasPermission) {
                    vm.locationError?.let { msg ->
                        Column {
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { vm.refresh(force = true) }) { Text("Tentar outra vez") }
                        }
                    }
                    PriceEvolution(vm, onOpenStation = { stationSheet = it })
                } else {
                    LocationPrompt {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            }

            // 3) Noticias sobre la próxima semana
            NewsCard(vm, myFuelId)
        }
    }

    stationSheet?.let { id ->
        StationSheet(vm, id, myFuelId, onDismiss = { stationSheet = null })
    }
}

/** Barra superior verde, igual que en el resto de pantallas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(vm: NearbyViewModel, onBack: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(20_000L)
        }
    }
    StatusBarIcons(darkBackground = true)
    TopAppBar(
        colors = atestaTopBarColors(),
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao mapa") }
        },
        title = {
            Column {
                Text("Preços e notícias", fontWeight = FontWeight.Bold)
                Text(
                    vm.pricesTime?.let { "Preços " + updatedAgo(it, now) } ?: "Preços dos combustíveis hoje",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        },
        actions = {
            IconButton(onClick = { vm.refresh(force = true) }) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar") }
        },
    )
}

/** Bloque completo para pedir la ubicación. */
@Composable
internal fun PermissionCard(onRequest: () -> Unit) {
    SectionCard(
        icon = "📍",
        accent = SectionColors.Teal,
        title = "Ativa a localização",
        subtitle = "Para veres as bombas perto de ti",
    ) {
        LocationPrompt(onRequest)
    }
}

/** Texto y botón para pedir la ubicación, dentro de una sección. */
@Composable
internal fun LocationPrompt(onRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Com a tua localização mostramos como mudam os preços das bombas da tua zona e onde fica a mais barata.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Usar a minha localização")
        }
    }
}

@Composable
private fun PriceEvolution(vm: NearbyViewModel, onOpenStation: (Long) -> Unit) {
    var fuelId by rememberSaveable { mutableIntStateOf(FUEL_TYPES.first().id) }
    val fuel = FUEL_TYPES.first { it.id == fuelId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FuelSelector(
            selectedId = fuelId,
            onSelect = { fuelId = it },
            hint = "Escolhe o combustível",
        )
        WeeklyCard(vm, fuel, onOpenStation)
    }
}

/** "agora mesmo", "há 5 min", "há 2 h". */
private fun updatedAgo(time: Long, now: Long): String {
    val min = ((now - time) / 60_000L).coerceAtLeast(0)
    return when {
        min < 1 -> "atualizado agora mesmo"
        min < 60 -> "atualizado há $min min"
        else -> "atualizado há ${min / 60} h"
    }
}
