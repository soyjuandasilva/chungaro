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
}
