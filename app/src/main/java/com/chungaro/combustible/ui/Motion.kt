package com.chungaro.combustible.ui

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/*
 * Movimiento de Atesta: pocas animaciones y cada una con un porqué.
 * - Pantallas: entran desde la derecha al ir hacia dentro y salen al volver
 *   (dice dónde estás).
 * - Ficha del posto: sube desde abajo, como una hoja encima de Início.
 * - Pestañas: fundido corto, sin desplazamiento (son hermanas, no capas).
 * - Lista del mapa: se despliega desde su cabecera.
 * - Precio: la cifra se desliza hacia abajo si baja y hacia arriba si sube.
 * Con «Remover animações» del sistema, todo cambia al instante.
 */
object Motion {
    /** Salida exponencial: arranca rápido y frena suave. */
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    const val SHORT = 160
    const val MEDIUM = 260
    const val LONG = 340

    fun <T> enter(reduce: Boolean, ms: Int = MEDIUM): FiniteAnimationSpec<T> = if (reduce) snap() else tween(ms, easing = EaseOut)
    fun <T> exit(reduce: Boolean, ms: Int = SHORT): FiniteAnimationSpec<T> = if (reduce) snap() else tween(ms)
}

/** true si el sistema tiene las animaciones desactivadas (Acessibilidade → Remover animações). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Cambio de pantalla: hacia dentro entra por la derecha; al volver, al revés. */
fun screenTransition(forward: Boolean, reduce: Boolean): ContentTransform {
    if (reduce) return EnterTransition.None togetherWith ExitTransition.None
    val dir = if (forward) 1 else -1
    return (slideInHorizontally(Motion.enter(false)) { w -> dir * w / 6 } + fadeIn(Motion.enter(false))) togetherWith
        (slideOutHorizontally(Motion.exit(false)) { w -> -dir * w / 12 } + fadeOut(Motion.exit(false)))
}

/** Cambio de pestaña: fundido corto. */
fun tabTransition(reduce: Boolean): ContentTransform =
    if (reduce) EnterTransition.None togetherWith ExitTransition.None
    else fadeIn(tween(Motion.MEDIUM, delayMillis = 60, easing = Motion.EaseOut)) togetherWith fadeOut(tween(90))

/** Ficha que sube desde abajo. */
fun sheetEnter(reduce: Boolean): EnterTransition =
    if (reduce) EnterTransition.None
    else slideInVertically(Motion.enter(false, Motion.LONG)) { h -> h / 3 } + fadeIn(Motion.enter(false, Motion.MEDIUM))

fun sheetExit(reduce: Boolean): ExitTransition =
    if (reduce) ExitTransition.None
    else slideOutVertically(Motion.exit(false, 200)) { h -> h / 4 } + fadeOut(Motion.exit(false, 200))
