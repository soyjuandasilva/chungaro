package com.chungaro.combustible.nearby

import com.chungaro.combustible.ui.Atesta
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

/** Qué tipo de noticia es un consejo: buena (baja), mala (sube), aviso o neutra. */
enum class Tone { GOOD, BAD, WARN, NEUTRAL }

/** Color del tono según el tema. */
@Composable
fun Tone.color(): Color = when (this) {
    Tone.GOOD -> Atesta.colors.down
    Tone.BAD -> Atesta.colors.up
    Tone.WARN -> Atesta.colors.warning
    Tone.NEUTRAL -> Atesta.colors.accent
}

/** Un consejo del ticker. [fuelId]/[stationId] indican qué abrir al tocarlo. */
data class Tip(
    val icon: ImageVector,
    val text: String,
    val tone: Tone = Tone.NEUTRAL,
    val fuelId: Int? = null,
    val stationId: Long? = null,
)

private const val TICK_MS = 5_000L

/** Franja animada con los consejos del momento, que van rotando solos. */
@Composable
fun TipsTicker(
    vm: NearbyViewModel,
    myFuelId: Int,
    tankLiters: Double,
    consumptionL100: Double,
    extraTips: List<Tip>,
    onOpenFuel: (Int) -> Unit,
    onOpenStation: (Long) -> Unit,
) {
    val tips = rememberTips(vm, myFuelId, tankLiters, consumptionL100) + extraTips
    if (tips.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val current = tips[index % tips.size]
    // Respeta el «tempo para agir» de Acessibilidade: con él, cada consejo dura más.
    val a11y = LocalAccessibilityManager.current
    val tick = remember(a11y) { a11y?.calculateRecommendedTimeoutMillis(TICK_MS, containsIcons = true, containsText = true) ?: TICK_MS }
    LaunchedEffect(tips.size, tick) {
        while (true) {
            delay(tick)
            index = (index + 1) % tips.size
        }
    }
    val accent = current.tone.color()
    Surface(
        onClick = {
            current.stationId?.let(onOpenStation) ?: current.fuelId?.let(onOpenFuel)
            index = (index + 1) % tips.size
        },
        shape = CardShape,
        color = Atesta.colors.card,
        border = BorderStroke(1.dp, Atesta.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "tip",
                modifier = Modifier.weight(1f),
            ) { tip ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(tip.icon, contentDescription = null, tint = tip.tone.color(), modifier = Modifier.size(24.dp))
                    Text(
                        tip.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            // Indicador de posición
            Column(
                Modifier.padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                tips.indices.take(8).forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == index % tips.size) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (i == index % tips.size) accent else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

/** Construye los consejos a partir de los precios de hoy y del histórico guardado. */
@Composable
private fun rememberTips(vm: NearbyViewModel, myFuelId: Int, tankLiters: Double, consumptionL100: Double): List<Tip> {
    val myFuel = FUEL_TYPES.firstOrNull { it.id == myFuelId } ?: FUEL_TYPES.first()
    val myStations by remember(myFuel.id) { vm.stationsHistory(myFuel.id) }.collectAsStateWithLifecycle(emptyList())
    // Media de la zona de cada combustible
    val zones = FUEL_TYPES.associate { f ->
        val z by remember(f.id) { vm.history(f.id, HistoryRecorder.ZONE_AVG) }.collectAsStateWithLifecycle(emptyList())
        f.id to z
    }
    val tips = mutableListOf<Tip>()
    val ready = vm.rows[myFuel.id] as? FuelRowState.Ready
    val names = vm.stationsOf(myFuel.id)?.associateBy { it.id }.orEmpty()
    fun pct(v: Double) = String.format(com.chungaro.combustible.ui.APP_LOCALE, "%.1f%%", abs(v))

    // 1) Semana: combustible del vehículo primero, luego los demás con cambios grandes
    val weeklyMine = computeWeekly(zones[myFuel.id].orEmpty(), myStations)
    when (weeklyMine.advice) {
        WeeklyAdvice.FILL_THIS_WEEK -> tips += Tip(Icons.AutoMirrored.Outlined.TrendingDown, "${myFuel.short} ${pct(weeklyMine.changePct!!)} mais barato que na semana passada: boa altura para abastecer.", Tone.GOOD, fuelId = myFuel.id)
        WeeklyAdvice.WAIT -> tips += Tip(Icons.AutoMirrored.Outlined.TrendingUp, "${myFuel.short} subiu ${pct(weeklyMine.changePct!!)} esta semana: se puderes, abastece só o necessário.", Tone.BAD, fuelId = myFuel.id)
        WeeklyAdvice.STABLE -> tips += Tip(Icons.AutoMirrored.Outlined.TrendingFlat, "${myFuel.short} estável esta semana (${pct(weeklyMine.changePct!!)}).", fuelId = myFuel.id)
        WeeklyAdvice.COLLECTING -> tips += Tip(Icons.Outlined.BarChart, "A recolher histórico de preços: dia ${weeklyMine.days.coerceAtLeast(1)} de $WEEK_DAYS. As tendências chegam em breve.")
    }
    // 2) Mejor precio y si compensa
    ready?.result?.cheapest?.let { c ->
        tips += Tip(Icons.Outlined.Savings, "${myFuel.short} mais barata: ${stationTitle(c.station)} a ${formatEuro(c.station.price)} (${formatKm(c.km)}).", fuelId = myFuel.id, stationId = c.station.id)
        val near = ready.result.nearest
        if (near != null && near.station.id != c.station.id) {
            tripAdvice(near, c, tankLiters, consumptionL100)?.let { a ->
                tips += if (a.worthIt) Tip(Icons.Outlined.CheckCircle, "Compensa ir à mais barata: poupas ${formatMoney(a.net)} ao encher ${tankLiters.toInt()} L.", Tone.GOOD, fuelId = myFuel.id)
                else Tip(Icons.Outlined.PanTool, "Não compensa ir à mais barata: o desvio come a poupança. Fica-te pela mais próxima.", Tone.WARN, stationId = near.station.id)
            }
        }
        ready.result.cheapestList.let { list ->
            if (list.size >= 2) {
                val max = list.maxOf { it.station.price }
                val saving = (max - c.station.price) * tankLiters
                if (saving >= 0.5) tips += Tip(Icons.Outlined.Savings, "Escolher bem a bomba poupa até ${formatMoney(saving)} por depósito na tua zona.", Tone.GOOD, fuelId = myFuel.id)
            }
        }
    }
    // 3) Bombas que bajaron / subieron esta semana
    weeklyMine.dropped.firstOrNull()?.let { d ->
        names[d.stationId]?.let { tips += Tip(Icons.Outlined.ArrowDownward, "${stationTitle(it)} baixou ${formatEuro(-d.diff)} no ${myFuel.short} esta semana.", Tone.GOOD, stationId = d.stationId) }
    }
    weeklyMine.risen.firstOrNull()?.let { r ->
        names[r.stationId]?.let { tips += Tip(Icons.Outlined.ArrowUpward, "${stationTitle(it)} subiu ${formatEuro(r.diff)} no ${myFuel.short} esta semana.", Tone.BAD, stationId = r.stationId) }
    }
    // 4) Otros combustibles con cambios semanales notables
    FUEL_TYPES.filter { it.id != myFuel.id }.forEach { f ->
        val p = computeWeekly(zones[f.id].orEmpty(), emptyList()).changePct ?: return@forEach
        if (abs(p) >= 1.5) {
            tips += if (p < 0) Tip(Icons.AutoMirrored.Outlined.TrendingDown, "${f.short} desceu ${pct(p)} face à semana passada.", Tone.GOOD, fuelId = f.id)
            else Tip(Icons.AutoMirrored.Outlined.TrendingUp, "${f.short} subiu ${pct(p)} face à semana passada.", Tone.BAD, fuelId = f.id)
        }
    }
    return tips
}
