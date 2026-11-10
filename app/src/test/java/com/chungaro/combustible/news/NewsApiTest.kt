package com.chungaro.combustible.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsApiTest {
    private val feed = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/"><channel>
        <title>"preço combustíveis próxima semana" - Google Notícias</title>
        <item>
          <title>Gasóleo vai descer na próxima semana - Jornal X</title>
          <link>https://news.google.com/rss/articles/abc?oc=5</link>
          <pubDate>Fri, 06 Nov 2026 10:30:00 GMT</pubDate>
          <description>&lt;a href="https://news.google.com/rss/articles/abc?oc=5" target="_blank"&gt;Gasóleo vai descer na próxima semana&lt;/a&gt;&amp;nbsp;&amp;nbsp;&lt;font color="#6f6f6f"&gt;Jornal X&lt;/font&gt;</description>
          <source url="https://jornalx.pt">Jornal X</source>
        </item>
        <item>
          <title>Gasolina sobe dois cêntimos a partir de segunda-feira - Rádio Y</title>
          <link>https://news.google.com/rss/articles/def?oc=5</link>
          <pubDate>Sat, 07 Nov 2026 08:00:00 GMT</pubDate>
          <description>&lt;p&gt;Os preços da gasolina vão aumentar cerca de dois cêntimos por litro na próxima semana, segundo fonte do setor.&lt;/p&gt;</description>
          <source url="https://radioy.pt">Rádio Y</source>
        </item>
        <item>
          <title>Duplicada - Jornal X</title>
          <link>https://news.google.com/rss/articles/abc?oc=5</link>
        </item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun parsesTitleSourceAndLinkWithoutDuplicates() {
        val items = NewsApi.parse(feed)
        assertEquals(2, items.size)
        assertEquals("Gasóleo vai descer na próxima semana", items[0].title)
        assertEquals("Jornal X", items[0].source)
        assertEquals("https://news.google.com/rss/articles/abc?oc=5", items[0].link)
        assertNotNull(items[0].publishedAt)
    }

    @Test
    fun noSnippetWhenDescriptionIsOnlyTitleAndSource() {
        assertEquals("", NewsApi.parse(feed)[0].snippet)
    }

    @Test
    fun keepsRealSnippet() {
        val s = NewsApi.parse(feed)[1].snippet
        assertTrue(s.startsWith("Os preços da gasolina vão aumentar"))
    }
}
