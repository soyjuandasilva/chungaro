package com.chungaro.combustible.nearby

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandStyleTest {
    @Test
    fun knownBrands() {
        assertEquals("G", brandStyle("GALP").label)
        assertEquals("G", brandStyle("Galp Energia").label)
        assertEquals("bp", brandStyle("BP").label)
        assertEquals("R", brandStyle("REPSOL").label)
        assertEquals("P", brandStyle("PRIO").label)
        assertEquals("I", brandStyle("INTERMARCHÉ").label)
        assertEquals("E", brandStyle("E.LECLERC").label)
        assertEquals("AB", brandStyle("ALVES BANDEIRA").label)
    }

    @Test
    fun genericAndUnknown() {
        assertEquals("⛽", brandStyle("Genérico").label)
        assertEquals("⛽", brandStyle("").label)
        assertEquals("PC", brandStyle("Posto Central").label)
        // El color de una marca desconocida es estable.
        assertEquals(brandStyle("Posto Central").background, brandStyle("posto central").background)
    }

    @Test
    fun shortKeysDoNotMatchInsideWords() {
        // "bp" no debe coincidir dentro de otra palabra.
        assertEquals("AB", brandStyle("Abpetro Bombas").label)
    }
}
