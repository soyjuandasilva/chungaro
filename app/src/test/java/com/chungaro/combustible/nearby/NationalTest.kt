package com.chungaro.combustible.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NationalTest {
    private fun station(id: Long, price: Double) = PricedStation(id, "", "", "", "", 38.7, -9.1, price, "")

    @Test
    fun nationalAverageOfAllStations() {
        val n = nationalAverage(listOf(station(1, 1.70), station(2, 1.80), station(3, 1.90)))!!
        assertEquals(1.80, n.average, 1e-9)
        assertEquals(3, n.stations)
    }

    @Test
    fun noStationsNoAverage() {
        assertNull(nationalAverage(emptyList()))
    }
}
