package com.chungaro.combustible.nearby

import com.chungaro.combustible.prices.DgegApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyTest {
    private fun st(id: Long, lat: Double, lon: Double, price: Double) =
        PricedStation(id, "S$id", "", "", "", lat, lon, price, "")

    @Test
    fun distanceLisbonPorto() {
        // Lisboa–Porto ≈ 274 km en línea recta
        val d = distanceKm(38.7223, -9.1393, 41.1579, -8.6291)
        assertEquals(274.0, d, 5.0)
    }

    @Test
    fun cheapestAndNearest() {
        val me = 38.7223 to -9.1393
        val stations = listOf(
            st(1, 38.7230, -9.1390, 1.80), // ~0,1 km, cara
            st(2, 38.7500, -9.1400, 1.70), // ~3 km, la más barata
            st(3, 38.7600, -9.1400, 1.70), // ~4 km, mismo precio pero más lejos
            st(4, 39.5000, -9.1400, 1.50), // ~86 km, fuera del radio
        )
        val r = computeNearby(stations, me.first, me.second, radiusKm = 10.0)
        assertEquals(1L, r.nearest!!.station.id)
        assertEquals(2L, r.cheapest!!.station.id)
        assertEquals(3, r.countInRadius)
        assertEquals(listOf(2L, 3L, 1L), r.cheapestList.map { it.station.id })
    }

    @Test
    fun nothingInRadius() {
        val r = computeNearby(listOf(st(1, 41.0, -8.6, 1.7)), 38.72, -9.14, 5.0)
        assertNull(r.cheapest)
        assertEquals(1L, r.nearest!!.station.id)
        assertNull(r.averageInRadius)
    }

    @Test
    fun parsesPricedPage() {
        val json = """
            {"status":true,"mensagem":"","resultado":[
              {"Id":10,"Nome":"A","Marca":"GALP","Municipio":"Lisboa","Distrito":"Lisboa","Morada":"Rua 1",
               "Localidade":"Lisboa","CodPostal":"1000","Latitude":38.72,"Longitude":-9.14,"Preco":"1,679 €","Quantidade":3},
              {"Id":11,"Nome":"B","Latitude":"38,75","Longitude":"-9,15","Preco":"1,701 €","Quantidade":3},
              {"Id":12,"Nome":"Sin coords","Latitude":0,"Longitude":0,"Preco":"1,6 €","Quantidade":3}
            ]}
        """.trimIndent()
        val (page, qty) = DgegApi.parsePricedStations(json)
        assertEquals(3, qty)
        assertEquals(3, page.rawCount)
        assertEquals(listOf(10L, 11L), page.stations.map { it.id })
        assertEquals(1.679, page.stations[0].price, 1e-9)
        assertEquals(38.75, page.stations[1].lat, 1e-9)
    }

    @Test
    fun tripAdviceWorthIt() {
        val near = StationAtDistance(st(1, 0.0, 0.0, 1.80), 1.0)
        val cheap = StationAtDistance(st(2, 0.0, 0.0, 1.70), 3.0)
        // 0,10 €/L x 50 L = 5 € ; desvío 2 km x 2 x 1,3 = 5,2 km ; 5,2 x 6/100 x 1,70 = 0,5304 €
        val a = tripAdvice(near, cheap, liters = 50.0, consumptionL100 = 6.0)!!
        assertEquals(5.0, a.savings, 1e-9)
        assertEquals(5.2, a.extraKm, 1e-9)
        assertEquals(0.5304, a.tripCost, 1e-9)
        assertEquals(true, a.worthIt)
    }

    @Test
    fun tripAdviceNotWorthIt() {
        val near = StationAtDistance(st(1, 0.0, 0.0, 1.705), 0.5)
        val cheap = StationAtDistance(st(2, 0.0, 0.0, 1.700), 15.0)
        val a = tripAdvice(near, cheap, liters = 40.0, consumptionL100 = 7.0)!!
        assertEquals(false, a.worthIt)
        assertNull(tripAdvice(near, near, 40.0, 7.0))
    }
}
