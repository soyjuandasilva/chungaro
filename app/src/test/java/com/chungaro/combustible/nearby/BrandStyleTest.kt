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
        // Sin marca: etiqueta vacía, se dibuja el icono de surtidor.
        assertEquals("", brandStyle("Genérico").label)
        assertEquals("", brandStyle("").label)
        assertEquals("PC", brandStyle("Posto Central").label)
        // El color de una marca desconocida es estable.
        assertEquals(brandStyle("Posto Central").background, brandStyle("posto central").background)
    }

    @Test
    fun shortKeysDoNotMatchInsideWords() {
        // "bp" no debe coincidir dentro de otra palabra.
        assertEquals("AB", brandStyle("Abpetro Bombas").label)
    }

    @Test
    fun logoDomains() {
        assertEquals("galp.com", brandStyle("GALP").domain)
        assertEquals("prio.pt", brandStyle("PRIO").domain)
        assertEquals(null, brandStyle("Genérico").domain)
        assertEquals(null, brandStyle("Posto Central").domain)
    }

    @Test
    fun bundledLogos() {
        assertEquals("intermarche", brandStyle("INTERMARCHÉ").logo)
        assertEquals("moeve", brandStyle("CEPSA").logo)
        assertEquals("petroprix", brandStyle("PETROPRIX").logo)
        assertEquals(null, brandStyle("BXPRESS").logo)
    }
}
