package com.chungaro.combustible.nearby

import com.chungaro.combustible.news.NewsItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MondayAdviceTest {
    private val now = 1_800_000_000_000L
    private fun news(title: String, daysAgo: Int = 1) =
        NewsItem(title, "https://x/$title", "Jornal", "", now - daysAgo * 86_400_000L)

    @Test
    fun readsDirectionPerFuel() {
        assertEquals(PriceDirection.UP, headlineDirection("Gasóleo vai subir na próxima semana", FuelFamily.DIESEL))
        assertNull(headlineDirection("Gasóleo vai subir na próxima semana", FuelFamily.GASOLINE))
        assertEquals(PriceDirection.DOWN, headlineDirection("Gasolina fica mais barata a partir de segunda-feira", FuelFamily.GASOLINE))
    }

    @Test
    fun splitsMixedHeadlines() {
        val t = "Gasóleo sobe dois cêntimos e gasolina desce um cêntimo"
        assertEquals(PriceDirection.UP, headlineDirection(t, FuelFamily.DIESEL))
        assertEquals(PriceDirection.DOWN, headlineDirection(t, FuelFamily.GASOLINE))
    }

    @Test
    fun genericHeadlinesApplyToAll() {
        assertEquals(PriceDirection.DOWN, headlineDirection("Preços dos combustíveis descem na segunda-feira", FuelFamily.LPG))
        assertNull(headlineDirection("O que muda na segunda-feira", FuelFamily.DIESEL))
    }

    @Test
    fun verdictFromRecentNews() {
        val items = listOf(
            news("Gasóleo vai subir na próxima semana", 1),
            news("Combustíveis: gasóleo com aumento de 3 cêntimos", 2),
            news("Gasóleo desce", 12), // demasiado antigua
        )
        val v = mondayVerdict(items, FuelFamily.DIESEL, now)
        assertEquals(MondayKind.FILL_NOW, v.kind)
        assertEquals(2, v.upVotes)
        assertEquals(0, v.downVotes)
        assertEquals("Gasóleo vai subir na próxima semana", v.evidence?.title)
    }

    @Test
    fun unclearWithoutSignals() {
        val v = mondayVerdict(listOf(news("Gasóleo sobe", 1), news("Gasóleo desce", 1)), FuelFamily.DIESEL, now)
        assertEquals(MondayKind.UNCLEAR, v.kind)
        assertEquals(MondayKind.UNCLEAR, mondayVerdict(emptyList(), FuelFamily.GASOLINE, now).kind)
    }

    @Test
    fun daysUntilMonday() {
        assertEquals(6, daysToMonday(LocalDate.of(2026, 11, 10))) // martes
        assertEquals(1, daysToMonday(LocalDate.of(2026, 11, 15))) // domingo
        assertEquals(7, daysToMonday(LocalDate.of(2026, 11, 16))) // lunes
    }
}
