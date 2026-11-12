package com.chungaro.combustible.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TourStep(val title: String, val body: String)

private val STEPS = listOf(
    TourStep(
        "Bem-vindo à Atesta",
        "Os preços oficiais de todos os postos de Portugal, para atestares onde fica mais barato.",
    ),
    TourStep(
        "Os postos à tua volta",
        "No mapa vês cada posto com o preço de hoje. Escolhe o combustível e o raio, e toca num posto para veres todos os preços e como lá chegar.",
    ),
    TourStep(
        "Compara e poupa",
        "Em Preços tens a média da tua zona e de Portugal e os postos mais baratos. A calculadora diz-te quanto poupas por depósito.",
    ),
    TourStep(
        "Abasteces hoje ou esperas por segunda?",
        "Os preços mudam à segunda-feira. Em Notícias lemos as previsões da semana e dizemos-te se compensa abastecer já ou esperar.",
    ),
    TourStep(
        "Favoritos e alertas",
        "Guarda os teus postos com a estrela e define um preço-alvo: avisamos-te quando o combustível descer.",
    ),
)

// El tour es una pieza de marca: siempre en verde oscuro, con el tema claro o el oscuro.
private val DeepGreen = Color(0xFF0B3A20)
private val TourTop = Color(0xFF08130E)
private val TourAccent = Color(0xFF4ADE80)
private val Brand = Color(0xFF15803D)
private val CardInk = Color(0xFF0F1A14)
private val CardMuted = Color(0xFF4A5E53)

/**
 * Tour de bienvenida: la primera vez que se abre la app (y desde Definições).
 * En la última página pide la ubicación. [onFinish] al terminar o saltar.
 */
@Composable
fun QuickTour(onFinish: () -> Unit) {
    StatusBarIcons(darkBackground = true)
    val pager = rememberPagerState { STEPS.size }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onFinish() }
    val last = pager.currentPage == STEPS.lastIndex
    val reduce = rememberReduceMotion()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TourTop, DeepGreen))),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_logo), contentDescription = null, tint = TourAccent, modifier = Modifier.size(22.dp))
                Text("Atesta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f).padding(start = 8.dp))
                if (!last) {
                    TextButton(onClick = onFinish) { Text("Saltar", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                TourPage(page, current = pager.currentPage == page, reduce = reduce)
            }

            // Puntos de página
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(STEPS.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, Motion.enter(reduce), label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = w, height = 8.dp)
                            .background(if (i == pager.currentPage) TourAccent else Color.White.copy(alpha = 0.3f), CircleShape),
                    )
                }
            }

            Button(
                onClick = {
                    if (last) {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else {
                        scope.launch { if (reduce) pager.scrollToPage(pager.currentPage + 1) else pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TourAccent, contentColor = DeepGreen),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            ) {
                Text(if (last) "Usar a minha localização e começar" else "Seguinte", fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                if (last) {
                    TextButton(onClick = onFinish) { Text("Agora não", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun TourPage(page: Int, current: Boolean, reduce: Boolean) {
    // El texto sube y aparece cuando la página está a la vista.
    val shown by animateFloatAsState(if (current || reduce) 1f else 0f, Motion.enter(reduce, 500), label = "shown")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            when (page) {
                0 -> WelcomeArt(reduce)
                1 -> MapArt(current, reduce)
                2 -> SavingsArt(current, reduce)
                3 -> MondayArt(current, reduce)
                else -> AlertArt(current, reduce)
            }
        }
        Spacer(Modifier.height(28.dp))
        val step = STEPS[page]
        Column(
            Modifier.graphicsLayer {
                alpha = shown
                translationY = (1f - shown) * 40f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                step.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
    }
}

/** 1) El logotipo con ondas suaves. */
@Composable
private fun WelcomeArt(reduce: Boolean) {
    val t = rememberInfiniteTransition(label = "welcome")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "wave")
    Box(contentAlignment = Alignment.Center) {
        if (!reduce) {
            listOf(0f, 0.5f).forEach { shift ->
                val p = (wave + shift) % 1f
                Box(
                    Modifier
                        .size(150.dp)
                        .scale(1f + p * 0.7f)
                        .graphicsLayer { alpha = (1f - p) * 0.35f }
                        .background(TourAccent, RoundedCornerShape(40.dp)),
                )
            }
        }
        // Igual que el icono de la app.
        Surface(shape = RoundedCornerShape(40.dp), color = Brand, shadowElevation = 8.dp, modifier = Modifier.size(150.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_logo), contentDescription = null, tint = Color.White, modifier = Modifier.size(84.dp))
            }
        }
    }
}

/** 2) Mini mapa: los postos aparecen uno a uno con su precio y tu posición late. */
@Composable
private fun MapArt(current: Boolean, reduce: Boolean) {
    val pins = listOf(
        Triple(Offset(-62f, -78f), "1,789", true),
        Triple(Offset(58f, -28f), "1,829", false),
        Triple(Offset(-46f, 58f), "1,849", false),
    )
    val scales = remember { pins.map { Animatable(if (reduce) 1f else 0f) } }
    LaunchedEffect(current) {
        if (reduce) return@LaunchedEffect
        scales.forEach { it.snapTo(0f) }
        if (!current) return@LaunchedEffect
        scales.forEach { a ->
            delay(220)
            launch { a.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        }
    }
    val t = rememberInfiniteTransition(label = "me")
    val pulse by t.animateFloat(0.8f, 1.5f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
    Surface(shape = RoundedCornerShape(32.dp), color = Color(0xFFE8F0EA), modifier = Modifier.size(260.dp)) {
        Box(contentAlignment = Alignment.Center) {
            // Carreteras
            Canvas(Modifier.fillMaxSize()) {
                val road = Color(0xFFFFFFFF)
                drawLine(road, Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.55f), 28f, StrokeCap.Round)
                drawLine(road, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.55f, size.height), 22f, StrokeCap.Round)
                drawLine(road, Offset(0f, size.height * 0.85f), Offset(size.width, size.height * 0.75f), 16f, StrokeCap.Round)
            }
            Box(Modifier.size(18.dp).scale(if (reduce) 1f else pulse).background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape))
            Box(Modifier.size(14.dp).background(Color.White, CircleShape).padding(3.dp).background(Color(0xFF2563EB), CircleShape))
            pins.forEachIndexed { i, (pos, price, cheapest) ->
                PricePin(price, cheapest, Modifier.offset(pos.x.dp, pos.y.dp).scale(scales[i].value))
            }
        }
    }
}

/**
 * Marcador como los del mapa: el precio en el panel negro de 7 segmentos. El más
 * barato lleva un aro verde y el surtidor.
 */
@Composable
private fun PricePin(price: String, cheapest: Boolean, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SegmentPanel,
        shadowElevation = 4.dp,
        border = if (cheapest) BorderStroke(2.dp, Brand) else null,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cheapest) {
                Icon(Icons.Outlined.LocalGasStation, contentDescription = null, tint = SegmentOn, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
            }
            SegmentDigits(price, 15.dp)
        }
    }
}

/** 3) Cuánto se ahorra por depósito: el contador sube (es un ejemplo). */
@Composable
private fun SavingsArt(current: Boolean, reduce: Boolean) {
    val value by animateFloatAsState(
        if (current || reduce) 6.40f else 0f,
        if (current && !reduce) tween(1400, easing = Motion.EaseOut) else tween(0),
        label = "savings",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.width(260.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Mais barato" to "1,789 €", "Média da zona" to "1,829 €", "Mais caro" to "1,917 €").forEachIndexed { i, (label, price) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = CardMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(price, color = if (i == 0) Brand else CardInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = TourAccent, modifier = Modifier.width(260.dp)) {
            Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format(APP_LOCALE, "+%.2f €", value),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepGreen,
                )
                Text("por depósito de 50 L (exemplo)", style = MaterialTheme.typography.labelLarge, color = DeepGreen)
            }
        }
    }
}

/** 4) El veredicto del lunes, como la tarjeta de Notícias. */
@Composable
private fun MondayArt(current: Boolean, reduce: Boolean) {
    val t = rememberInfiniteTransition(label = "monday")
    val bounce by t.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = Motion.EaseOut), RepeatMode.Reverse), label = "bounce")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(width = 128.dp, height = 136.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(40.dp).background(Brand), contentAlignment = Alignment.Center) {
                        Text("Segunda", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("€", color = DeepGreen, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displayMedium)
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Icon(
                Icons.AutoMirrored.Outlined.TrendingDown,
                contentDescription = null,
                tint = TourAccent,
                modifier = Modifier.size(72.dp).graphicsLayer { translationY = if (reduce || !current) 0f else bounce * 18f },
            )
        }
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.width(260.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("Se puderes, espera por segunda", color = CardInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("As notícias apontam para uma descida do gasóleo.", color = CardMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 5) Un aviso de precio que llega al móvil. */
@Composable
private fun AlertArt(current: Boolean, reduce: Boolean) {
    val slide = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(current) {
        if (reduce) return@LaunchedEffect
        slide.snapTo(0f)
        if (current) {
            delay(250)
            slide.animateTo(1f, tween(Motion.LONG, easing = Motion.EaseOut))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(270.dp)
                .graphicsLayer {
                    alpha = slide.value
                    translationY = (1f - slide.value) * -60f
                },
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Brand, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_logo), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Gasolina 95 a 1,749 €", color = CardInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Está abaixo do teu alerta (1,760 €).", color = CardMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
