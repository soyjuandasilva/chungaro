package com.chungaro.combustible.nearby

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationInfoTest {
    private fun lisbon(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.of("Europe/Lisbon")).toInstant().toEpochMilli()

    @Test
    fun parsesDgegDates() {
        val expected = lisbon(2026, 11, 10, 8, 15)
        assertEquals(expected, parseDgegDate("2026-11-10 08:15"))
        assertEquals(expected, parseDgegDate("2026-11-10 08:15:00"))
        assertEquals(expected, parseDgegDate("10-11-2026 08:15"))
        assertEquals(expected, parseDgegDate("2026-11-10T08:15:00.000"))
        assertNull(parseDgegDate("ontem"))
    }

    @Test
    fun agoTexts() {
        val now = lisbon(2026, 11, 10, 18, 15)
        assertEquals("há 10 h", updatedText("2026-11-10 08:15", now))
        assertEquals("há 25 min", agoText(now - 25 * 60_000L, now))
        assertEquals("há 7 dias", agoText(now - 7 * 24 * 3_600_000L, now))
        assertEquals("sem data", updatedText("sem data", now))
    }

    @Test
    fun formatsOpeningHours() {
        assertEquals(listOf("Aberto 24 horas, todos os dias"), OpeningHours.format("24/7"))
        assertEquals(
            listOf("Seg–Sex: 07:00–22:00", "Sáb, Dom: 08:00–20:00"),
            OpeningHours.format("Mo-Fr 07:00-22:00; Sa,Su 08:00-20:00"),
        )
        assertEquals(listOf("Dom: Fechado"), OpeningHours.format("Su off"))
        assertEquals(listOf("07:00–23:00"), OpeningHours.format("07:00-23:00"))
    }

    @Test
    fun picksNearestStationWithHours() {
        val json = """
            {"elements":[
              {"type":"node","lat":40.7400,"lon":-8.6400,"tags":{"amenity":"fuel","opening_hours":"24/7"}},
              {"type":"way","center":{"lat":40.7301,"lon":-8.6301},"tags":{"amenity":"fuel","opening_hours":"Mo-Su 07:00-22:00"}},
              {"type":"node","lat":40.7300,"lon":-8.6300,"tags":{"amenity":"fuel"}}
            ]}
        """.trimIndent()
        assertEquals("Mo-Su 07:00-22:00", OpeningHours.parse(json, 40.7300, -8.6300))
        assertNull(OpeningHours.parse("""{"elements":[]}""", 40.73, -8.63))
    }
}
