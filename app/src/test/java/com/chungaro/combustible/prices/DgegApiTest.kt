package com.chungaro.combustible.prices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DgegApiTest {
    @Test
    fun parsesPrices() {
        assertEquals(1.749, DgegApi.parsePrice("1,749 €/litro")!!, 1e-9)
        assertEquals(0.859, DgegApi.parsePrice("0.859 €/litro")!!, 1e-9)
        assertNull(DgegApi.parsePrice(""))
    }

    @Test
    fun parsesStationList() {
        val json = """
            {"status":true,"resultado":[
              {"Id":123,"Nome":"Posto A","Marca":"GALP","Municipio":"Lisboa","Localidade":"Lisboa","Morada":"Rua X, 1"},
              {"Id":null,"Nome":"Sin id"},
              {"Id":456,"Nome":"Posto B","Marca":null,"Municipio":"Sintra","Localidade":"Cacém","Morada":null}
            ]}
        """.trimIndent()
        val list = DgegApi.parseStations(json)
        assertEquals(2, list.size)
        assertEquals("Rua X, 1", list[0].address)
        assertEquals("", list[1].brand)
        assertEquals("", list[1].address)
    }

    @Test
    fun parsesStationDetail() {
        val json = """
            {"status":true,"resultado":{
              "Nome":"Posto A","Marca":"GALP","TipoPosto":"Outro",
              "Morada":{"Morada":"Rua X, 1","Localidade":"Lisboa","CodPostal":"1000-001","Latitude":"38.7","Longitude":"-9.1"},
              "Combustiveis":[
                {"TipoCombustivel":"Gasóleo simples","Preco":"1,649 €/litro","DataAtualizacao":"2026-11-09 08:00"},
                {"TipoCombustivel":"Gasolina simples 95","Preco":"1,789 €/litro","DataAtualizacao":"2026-11-09 08:00"}
              ]}}
        """.trimIndent()
        val d = DgegApi.parseStation(123, json)
        assertEquals("Rua X, 1, Lisboa, 1000-001", d.address)
        assertEquals(2, d.fuels.size)
        assertEquals("Gasóleo simples", d.fuels[0].fuel)
        assertEquals(1.789, d.fuels[1].price!!, 1e-9)
    }

    @Test
    fun removesDuplicateStations() {
        val json = """
            {"resultado":[
              {"Id":1,"Nome":"A","Combustivel":"Gasóleo simples","Distrito":"Aveiro"},
              {"Id":1,"Nome":"A","Combustivel":"Gasolina simples 95","Distrito":"Aveiro"},
              {"Id":2,"Nome":"B","Distrito":"Bragança"}
            ]}
        """.trimIndent()
        val list = DgegApi.parseStations(json)
        assertEquals(listOf(1L, 2L), list.map { it.id })
        assertEquals(listOf(1L), DgegApi.filterByDistrict(list, "Aveiro").map { it.id })
        assertEquals(listOf(2L), DgegApi.filterByDistrict(list, "Braganca").map { it.id })
    }

    @Test
    fun doesNotFilterWhenDistrictMissing() {
        val list = listOf(Station(1, "A", "", "", "", ""), Station(2, "B", "", "", "", ""))
        assertEquals(2, DgegApi.filterByDistrict(list, "Aveiro").size)
    }
}
