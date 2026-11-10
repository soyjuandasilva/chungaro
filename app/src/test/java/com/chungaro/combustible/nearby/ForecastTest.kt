package com.chungaro.combustible.nearby

import com.chungaro.combustible.data.PricePoint
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastTest {
    private fun series(vararg p: Double) = p.mapIndexed { i, v -> PricePoint(200L + i, 3201, HistoryRecorder.NATIONAL_AVG, v) }

    private fun station(price: Double) = PricedStation(1, "", "", "", "", 38.7, -9.1, price, "")

    @Test
    fun nationalAverageOfAllStations() {
        val n = nationalAverage(listOf(station(1.70), station(1.80), station(1.90)))!!
        assertEquals(1.80, n.average, 1e-9)
        assertEquals(3, n.stations)
        assertNull(nationalAverage(emptyList()))
    }

    @Test
    fun needsAboutAWeekOfHistory() {
        assertNull(forecastNextWeek(series(1.70, 1.71, 1.72, 1.73), 1.73))
    }

    @Test
    fun risingTrendForecastsHigher() {
        val f = forecastNextWeek(series(1.70, 1.705, 1.71, 1.715, 1.72, 1.725, 1.73, 1.735), 1.735)!!
        assertTrue(f.changePct > 0)
        assertTrue(f.price > 1.735)
    }

    @Test
    fun fallingTrendForecastsLower() {
        val f = forecastNextWeek(series(1.80, 1.79, 1.78, 1.77, 1.76, 1.75, 1.74), 1.74)!!
        assertTrue(f.changePct < 0)
        assertTrue(f.price < 1.74)
    }

    @Test
    fun flatTrendForecastsSamePrice() {
        val f = forecastNextWeek(series(1.75, 1.75, 1.75, 1.75, 1.75, 1.75, 1.75), 1.75)!!
        assertEquals(0.0, f.changePct, 1e-9)
        assertEquals(1.75, f.price, 1e-9)
    }

    @Test
    fun changeIsCapped() {
        val f = forecastNextWeek(series(1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2), 2.2)!!
        assertEquals(4.0, f.changePct, 1e-9)
    }

    @Test
    fun nextMondayIsNeverToday() {
        assertEquals(LocalDate.of(2026, 11, 16), nextMonday(LocalDate.of(2026, 11, 10))) // martes
        assertEquals(LocalDate.of(2026, 11, 16), nextMonday(LocalDate.of(2026, 11, 9))) // lunes
    }
}
