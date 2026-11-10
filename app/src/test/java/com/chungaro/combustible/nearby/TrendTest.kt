package com.chungaro.combustible.nearby

import com.chungaro.combustible.data.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrendTest {
    private fun series(vararg prices: Double) = prices.mapIndexed { i, p -> PricePoint(100L + i, 3201, 0, p) }

    @Test
    fun collectingWithFewDays() {
        val t = computeTrend(series(1.70, 1.71))
        assertEquals(Advice.COLLECTING, t.advice)
        assertEquals(2, t.days)
        assertNull(computeTrend(emptyList()).last)
    }

    @Test
    fun risingSaysBuyNow() {
        val t = computeTrend(series(1.70, 1.70, 1.71, 1.72, 1.73, 1.74, 1.75, 1.76))
        assertEquals(Advice.BUY_NOW_RISING, t.advice)
        // Respecto a hace 7 días: 1.70 -> 1.76
        assertEquals((1.76 - 1.70) / 1.70 * 100, t.changePct!!, 1e-9)
    }

    @Test
    fun fallingToLowest() {
        val t = computeTrend(series(1.80, 1.79, 1.78, 1.76, 1.75, 1.74, 1.73, 1.72))
        assertEquals(Advice.LOWEST, t.advice)
    }

    @Test
    fun stable() {
        val t = computeTrend(series(1.70, 1.701, 1.699, 1.70, 1.702))
        assertEquals(Advice.STABLE, t.advice)
    }

    @Test
    fun recorderZonePoints() {
        val s = listOf(
            PricedStation(1, "A", "", "", "", 38.72, -9.14, 1.70, ""),
            PricedStation(2, "B", "", "", "", 38.73, -9.14, 1.80, ""),
            PricedStation(3, "Far", "", "", "", 41.0, -8.6, 1.50, ""),
        )
        val pts = HistoryRecorder.points(200, 3201, s, 38.72, -9.14)
        assertEquals(4, pts.size)
        assertEquals(1.75, pts.first { it.stationId == HistoryRecorder.ZONE_AVG }.price, 1e-9)
        assertEquals(1.70, pts.first { it.stationId == HistoryRecorder.ZONE_MIN }.price, 1e-9)
    }
}
