package com.chungaro.combustible.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {
    private val today = LocalDate.of(2026, 11, 9)
    private fun r(day: LocalDate, odo: Double, qty: Double, cost: Double, full: Boolean = true) =
        Refuel(epochDay = day.toEpochDay(), odometer = odo, quantity = qty, totalCost = cost, fullTank = full)

    @Test
    fun noRefuels() {
        val s = computeStats(emptyList(), today)
        assertEquals(0, s.summary.count)
        assertNull(s.summary.avgEfficiency)
    }

    @Test
    fun fullToFull() {
        val s = computeStats(
            listOf(
                r(today.minusDays(10), 1000.0, 40.0, 200.0),
                r(today.minusDays(3), 1500.0, 50.0, 250.0),
            ),
            today,
        )
        assertNull(s.rows[0].efficiency)
        assertEquals(500.0, s.rows[1].distance!!, 1e-9)
        assertEquals(10.0, s.rows[1].efficiency!!, 1e-9)
        assertEquals(0.5, s.rows[1].costPerKm!!, 1e-9)
        assertEquals(10.0, s.summary.avgEfficiency!!, 1e-9)
        assertEquals(450.0, s.summary.totalCost, 1e-9)
        assertEquals(5.0, s.summary.avgPrice!!, 1e-9)
    }

    @Test
    fun partialFillsAccumulateUntilNextFull() {
        val s = computeStats(
            listOf(
                r(today.minusDays(20), 0.0, 40.0, 200.0),
                r(today.minusDays(15), 300.0, 20.0, 100.0, full = false),
                r(today.minusDays(5), 600.0, 30.0, 150.0),
            ),
            today,
        )
        assertNull(s.rows[1].efficiency)
        // 600 km / (20 + 30) = 12 km/L ; 250 / 600 por km
        assertEquals(12.0, s.rows[2].efficiency!!, 1e-9)
        assertEquals(250.0 / 600.0, s.rows[2].costPerKm!!, 1e-9)
        assertEquals(600.0, s.summary.totalDistance, 1e-9)
    }

    @Test
    fun monthTotalsOnlyCountCurrentMonth() {
        val s = computeStats(
            listOf(
                r(LocalDate.of(2026, 10, 30), 0.0, 40.0, 200.0),
                r(LocalDate.of(2026, 11, 2), 400.0, 35.0, 180.0),
            ),
            today,
        )
        assertEquals(180.0, s.summary.monthCost, 1e-9)
        assertEquals(35.0, s.summary.monthQuantity, 1e-9)
    }
}
