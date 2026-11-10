package com.chungaro.combustible.news

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element

/** Noticia de Google News. */
data class NewsItem(
    val title: String,
    val link: String,
    val source: String,
    /** Extracto, si el feed trae algo más que el título; vacío si no. */
    val snippet: String,
    /** Momento de publicación en milisegundos, o null si no se pudo leer. */
    val publishedAt: Long?,
)

/**
 * Noticias sobre el precio de los combustibles la próxima semana, del feed RSS
 * público de Google News Portugal (la búsqueda web de Google no admite consultas
 * automáticas).
 */
object NewsApi {
    const val QUERY = "preço combustíveis próxima semana"

    private fun feedUrl(query: String): String =
        "https://news.google.com/rss/search?q=" + URLEncoder.encode(query, "UTF-8") +
            "&hl=pt-PT&gl=PT&ceid=PT:pt-150"

    suspend fun latest(limit: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        parse(get(feedUrl(QUERY))).take(limit)
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("User-Agent", "Atesta-Android/1.0")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // --- Parseo (público para poder probarlo) ---

    fun parse(xml: String): List<NewsItem> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val doc = factory.newDocumentBuilder().parse(xml.byteInputStream(Charsets.UTF_8))
        val items = doc.getElementsByTagName("item")
        return (0 until items.length).mapNotNull { i ->
            val e = items.item(i) as? Element ?: return@mapNotNull null
            val rawTitle = e.text("title")
            val link = e.text("link")
            if (rawTitle.isBlank() || link.isBlank()) return@mapNotNull null
            val source = e.text("source").ifBlank { rawTitle.substringAfterLast(" - ", "") }
            // Google News añade " - Medio" al final del título.
            val title = if (source.isNotBlank() && rawTitle.endsWith(" - $source")) rawTitle.removeSuffix(" - $source") else rawTitle
            NewsItem(
                title = title.trim(),
                link = link.trim(),
                source = source.trim(),
                snippet = snippet(e.text("description"), title, source),
                publishedAt = parseDate(e.text("pubDate")),
            )
        }.distinctBy { it.link }
    }

    private fun Element.text(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()

    /**
     * La descripción es HTML. En Google News suele ser solo el título y el medio;
     * en ese caso no hay extracto útil y se devuelve vacío.
     */
    fun snippet(html: String, title: String, source: String): String {
        val text = html
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
        val rest = text.replace(title.trim(), "").replace(source.trim(), "").replace(Regex("[\\s\\-–·|]+"), " ").trim()
        if (rest.length < 25) return ""
        return if (text.length > 220) text.take(217).trimEnd() + "…" else text
    }

    fun parseDate(s: String): Long? =
        runCatching { ZonedDateTime.parse(s.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
}
