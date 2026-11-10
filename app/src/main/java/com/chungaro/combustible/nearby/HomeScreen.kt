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

private val HeaderBrush = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF0E7490), Color(0xFF1D4ED8)))

@Composable
fun HomeScreen(
    vm: NearbyViewModel,
    myFuelId: Int,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
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

    Scaffold(bottomBar = bottomBar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(vm, onOpenSettings)

            // 1) Precio medio de hoy en todo el país, en grande
            NationalPricesCard(vm, myFuelId)

            // 2) Evolución de precios en tu zona (necesita la ubicación)
            SectionHeader(
                "Evolução dos preços",
                "Como mudaram os preços de cada combustível e de cada bomba da tua zona face à semana passada.",
            )
            if (hasPermission) {
                vm.locationError?.let { msg ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(msg, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { vm.refresh(force = true) }) { Text("Tentar outra vez") }
                        }
                    }
                }
                PriceEvolution(vm, onOpenStation = { stationSheet = it })
            } else {
                PermissionCard {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }

            // 3) Noticias sobre la próxima semana
            NewsCard(vm)
        }
    }

    stationSheet?.let { id ->
        StationSheet(vm, id, myFuelId, onDismiss = { stationSheet = null })
    }
}

@Composable
private fun Header(vm: NearbyViewModel, onOpenSettings: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(20_000L)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        // Ilustración (si se ha descargado) con un velo para que el texto se lea; si no, el dibujo.
        val context = LocalContext.current
        val artId = remember { context.resources.getIdentifier("header_art", "drawable", context.packageName) }
        if (artId != 0) {
            Image(
                painterResource(artId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(Color(0xD9042F2E), Color(0x80042F2E), Color(0x00042F2E))),
                ),
            )
        } else {
            Box(Modifier.matchParentSize().background(HeaderBrush).drawBehind { drawHeaderScene() })
        }
        Column(
            Modifier.padding(start = 18.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_fuel),
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    val sub = when {
                        vm.locating -> "A procurar onde estás…"
                        vm.pricesTime != null -> "Preços dos combustíveis · " + updatedAgo(vm.pricesTime!!, now)
                        else -> "Preços dos combustíveis hoje"
                    }
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(onClick = { vm.refresh(force = true) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = Color.White)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Definições", tint = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun PermissionCard(onRequest: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Text("Ativa a localização", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Com a tua localização mostramos como mudam os preços das bombas da tua zona e onde fica a mais barata. Preços oficiais da DGEG.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Usar a minha localização") }
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
            hint = "Escolhe o tipo de combustível para veres como mudou o preço",
        )
        WeeklyCard(vm, fuel, onOpenStation)
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
