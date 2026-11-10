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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.R
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class TourStep(val title: String, val body: String)

private val STEPS = listOf(
    TourStep(
        "Bem-vindo à Atesta",
        "Os preços oficiais de todas as bombas de Portugal, atualizados e sempre à mão.",
    ),
    TourStep(
        "Vê no mapa onde é mais barato",
        "Verde é barato, vermelho é caro. Escolhe o combustível e o raio, e toca numa bomba para veres a ficha e como lá chegar.",
    ),
    TourStep(
        "Abastece hoje ou espera por segunda?",
        "Em Portugal os preços mudam à segunda-feira. Lemos as notícias da semana e dizemos-te se compensa encher já ou esperar.",
    ),
    TourStep(
        "O teu carro, as tuas poupanças",
        "Em «Meu veículo» registas os abastecimentos e vês o consumo, os gastos e quanto poupas ao escolher bem a bomba.",
    ),
)

private val DeepGreen = Color(0xFF0B3A20)

/**
 * Tour de bienvenida: se muestra la primera vez que se abre la app (y desde el
 * menú). En la última pantalla pide la ubicación. [onFinish] al terminar o saltar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickTour(onFinish: () -> Unit) {
    StatusBarIcons(darkBackground = true)
    val pager = rememberPagerState { STEPS.size }
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onFinish() }
    val last = pager.currentPage == STEPS.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AtestaGreen, DeepGreen))),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Atesta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
                if (!last) {
                    TextButton(onClick = onFinish) { Text("Saltar", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold) }
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                TourPage(page, current = pager.currentPage == page)
            }

            // Puntos de página
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                repeat(STEPS.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = w, height = 8.dp)
                            .background(if (i == pager.currentPage) AtestaGreenLight else Color.White.copy(alpha = 0.3f), CircleShape),
                    )
                }
            }

            Button(
                onClick = {
                    if (last) {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtestaGreenLight, contentColor = DeepGreen),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            ) {
                Text(if (last) "Usar a minha localização e começar" else "Seguinte", fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                if (last) {
                    TextButton(onClick = onFinish) { Text("Agora não", color = Color.White.copy(alpha = 0.8f)) }
                }
            }
        }
    }
}

@Composable
private fun TourPage(page: Int, current: Boolean) {
    // Entrada del texto: sube y aparece cuando la página está a la vista.
    val shown by animateFloatAsState(if (current) 1f else 0f, tween(600), label = "shown")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            when (page) {
                0 -> WelcomeArt()
                1 -> MapArt(current)
                2 -> MondayArt()
                else -> SavingsArt(current)
            }
        }
        Spacer(Modifier.height(28.dp))
        val step = STEPS[page]
        Column(
            Modifier.graphicsLayer {
                alpha = shown
                translationY = (1f - shown) * 60f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
    }
}

/** 1) Logotipo con ondas que laten. */
@Composable
private fun WelcomeArt() {
    val t = rememberInfiniteTransition(label = "welcome")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "wave")
    Box(contentAlignment = Alignment.Center) {
        listOf(0f, 0.5f).forEach { shift ->
            val p = (wave + shift) % 1f
            Box(
                Modifier
                    .size(140.dp)
                    .scale(1f + p * 0.9f)
                    .graphicsLayer { alpha = (1f - p) * 0.5f }
                    .background(AtestaGreenLight, CircleShape),
            )
        }
        Surface(shape = CircleShape, color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(140.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_fuel), contentDescription = null, tint = AtestaGreen, modifier = Modifier.size(72.dp))
            }
        }
    }
}

/** 2) Mini mapa: las burbujas de precio caen una a una y tu posición late. */
@Composable
private fun MapArt(current: Boolean) {
    val bubbles = listOf(
        Triple(Offset(-70f, -70f), "1,789", Color(0xFF16A34A)),
        Triple(Offset(60f, -30f), "1,829", Color(0xFFF59E0B)),
        Triple(Offset(-40f, 60f), "1,899", Color(0xFFDC2626)),
    )
    val scales = remember { bubbles.map { Animatable(0f) } }
    LaunchedEffect(current) {
        scales.forEach { it.snapTo(0f) }
        if (!current) return@LaunchedEffect
        scales.forEach { a ->
            delay(250)
            launch { a.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        }
    }
    val t = rememberInfiniteTransition(label = "me")
    val pulse by t.animateFloat(0.8f, 1.4f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    Surface(shape = RoundedCornerShape(32.dp), color = Color(0xFFE8F5E9), modifier = Modifier.size(260.dp)) {
        Box(contentAlignment = Alignment.Center) {
            // Carreteras
            Canvas(Modifier.fillMaxSize()) {
                val road = Color(0xFFC8E6C9)
                drawLine(road, Offset(0f, size.height * 0.3f), Offset(size.width, size.height * 0.55f), 28f, StrokeCap.Round)
                drawLine(road, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.55f, size.height), 22f, StrokeCap.Round)
                drawLine(road, Offset(0f, size.height * 0.85f), Offset(size.width, size.height * 0.75f), 16f, StrokeCap.Round)
            }
            Box(Modifier.size(18.dp).scale(pulse).background(Color(0xFF2563EB).copy(alpha = 0.25f), CircleShape))
            Box(Modifier.size(14.dp).background(Color.White, CircleShape).padding(3.dp).background(Color(0xFF2563EB), CircleShape))
            bubbles.forEachIndexed { i, (pos, price, color) ->
                PriceBubble(price, color, Modifier.offset(pos.x.dp, pos.y.dp).scale(scales[i].value))
            }
        }
    }
}

@Composable
private fun PriceBubble(price: String, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(50), color = color, shadowElevation = 4.dp, modifier = modifier) {
        Row(Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(Color.White, CircleShape))
            Text(" $price", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** 3) Calendario del lunes con flecha que rebota y el cintillo de noticias. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MondayArt() {
    val t = rememberInfiniteTransition(label = "monday")
    val bounce by t.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bounce")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(width = 130.dp, height = 140.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF7C2D12)), contentAlignment = Alignment.Center) {
                        Text("SEGUNDA", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("€", color = AtestaGreen, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displayMedium)
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Text(
                "↘",
                color = AtestaGreenLight,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.graphicsLayer { translationY = bounce * 30f },
            )
        }
        Spacer(Modifier.height(28.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF4DC), modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(Color(0xFF7C2D12)).padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text("SEG", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    "⏳ Gasóleo: se puderes, espera por segunda, vai descer   ·   ⛽ Gasolina: abastece antes de segunda      ",
                    color = Color(0xFF7C2D12),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp).basicMarquee(iterations = Int.MAX_VALUE),
                )
            }
        }
    }
}

/** 4) Contador de poupança que sube y un coche que avanza. */
@Composable
private fun SavingsArt(current: Boolean) {
    val value by animateFloatAsState(if (current) 12.40f else 0f, tween(if (current) 1600 else 0), label = "savings")
    val t = rememberInfiniteTransition(label = "car")
    val drive by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "drive")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🚗", style = MaterialTheme.typography.displayLarge, modifier = Modifier.graphicsLayer { translationX = drive * 60f })
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.width(260.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Poupança por depósito", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                Text(
                    String.format(Locale.getDefault(), "+%.2f €", value),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF16A34A),
                )
                Text("ao escolher a bomba mais barata", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4B5F5C))
            }
        }
    }
}
