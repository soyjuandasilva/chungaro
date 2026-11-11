package com.chungaro.combustible.nearby

import com.chungaro.combustible.data.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyTest {
    private fun zone(vararg p: Double) = p.mapIndexed { i, v -> PricePoint(100L + i, 2101, 0, v) }

    @Test
    fun collectingWithoutAWeek() {
        val w = computeWeekly(zone(1.70, 1.71, 1.72), emptyList())
        assertEquals(WeeklyAdvice.COLLECTING, w.advice)
        assertEquals(3, w.days)
        assertNull(w.changePct)
    }

    @Test
    fun cheaperThanLastWeekSaysFill() {
        // día 100: 1.80 ... día 107: 1.75 (-2,8 %)
        val w = computeWeekly(zone(1.80, 1.80, 1.79, 1.78, 1.77, 1.77, 1.76, 1.75), emptyList())
        assertEquals(WeeklyAdvice.FILL_THIS_WEEK, w.advice)
        assertEquals((1.75 - 1.80) / 1.80 * 100, w.changePct!!, 1e-9)
    }

    @Test
    fun pricierSaysWait() {
        val w = computeWeekly(zone(1.70, 1.70, 1.71, 1.72, 1.72, 1.73, 1.74, 1.75), emptyList())
        assertEquals(WeeklyAdvice.WAIT, w.advice)
    }

    @Test
    fun stationChanges() {
        val z = zone(1.80, 1.80, 1.80, 1.80, 1.80, 1.80, 1.80, 1.80)
        val st = listOf(
            PricePoint(100, 2101, 1, 1.80), PricePoint(107, 2101, 1, 1.75), // bajó 0,05
            PricePoint(100, 2101, 2, 1.80), PricePoint(107, 2101, 2, 1.83), // subió 0,03
            PricePoint(100, 2101, 3, 1.80), PricePoint(107, 2101, 3, 1.80), // igual
            PricePoint(103, 2101, 4, 1.80), PricePoint(107, 2101, 4, 1.70), // sin semana completa
        )
        val w = computeWeekly(z, st)
        assertEquals(WeeklyAdvice.STABLE, w.advice)
        assertEquals(listOf(1L), w.dropped.map { it.stationId })
        assertEquals(listOf(2L), w.risen.map { it.stationId })
        assertEquals(-0.05, w.dropped[0].diff, 1e-9)
    }
}
