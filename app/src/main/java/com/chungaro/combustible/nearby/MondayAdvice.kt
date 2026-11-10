package com.chungaro.combustible.nearby

import com.chungaro.combustible.news.NewsItem
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * «Abastece hoje ou espera por segunda?»: en Portugal los precios cambian los
 * lunes y cada semana los medios publican si van a subir o bajar. Se leen los
 * titulares recientes y se da un veredicto para el combustible elegido.
 */

enum class FuelFamily { GASOLINE, DIESEL, LPG }

fun familyOf(fuelId: Int): FuelFamily = when (fuelId) {
    2101, 2105 -> FuelFamily.DIESEL
    1120 -> FuelFamily.LPG
    else -> FuelFamily.GASOLINE
}

enum class PriceDirection { UP, DOWN }

enum class MondayKind {
    /** Va a subir: mejor llenar antes del lunes. */
    FILL_NOW,
    /** Va a bajar: si puedes, espera al lunes. */
    WAIT,
    /** Sin previsión clara. */
    UNCLEAR,
}

data class MondayVerdict(
    val kind: MondayKind,
    /** Titular que respalda el veredicto (el más reciente en esa dirección). */
    val evidence: NewsItem?,
    /** Cuántos titulares recientes apuntan en cada dirección. */
    val upVotes: Int,
    val downVotes: Int,
)

private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private val UP_WORDS = listOf(
    "sobe", "sobem", "subir", "subida", "subidas", "aumenta", "aumentam", "aumentar", "aumento", "aumentos",
    "mais caro", "mais cara", "mais caros", "mais caras", "encarece", "encarecem", "agravamento", "dispara", "disparam",
)
private val DOWN_WORDS = listOf(
    "desce", "descem", "descer", "descida", "descidas", "baixa", "baixam", "baixar", "mais barato", "mais barata",
    "mais baratos", "mais baratas", "recua", "recuam", "alivio", "cai", "caem", "reducao", "reduz", "reduzem",
)
private val GASOLINE_WORDS = listOf("gasolina", "gasolinas")
private val DIESEL_WORDS = listOf("gasoleo", "diesel")
private val LPG_WORDS = listOf("gpl", "autogas")
private val GENERIC_WORDS = listOf("combustivel", "combustiveis", "precos", "preco", "bombas", "carburantes")

private fun hasWord(text: String, words: List<String>): Boolean =
    words.any { w -> Regex("(^|[^a-z])" + Regex.escape(w) + "($|[^a-z])").containsMatchIn(text) }

/**
 * Dirección que un titular anuncia para [family], o null si no dice nada claro.
 * Se parte en frases («Gasóleo sobe e gasolina desce») y cada verbo se asigna
 * al combustible que se nombra en su frase.
 */
fun headlineDirection(title: String, family: FuelFamily): PriceDirection? {
    val text = norm(title)
    val clauses = text.split(Regex("[,;:.!?]| e | mas | enquanto "))
    var up = 0
    var down = 0
    // Si ninguna frase nombra un combustible concreto, el titular habla de todos.
    val anySpecific = hasWord(text, GASOLINE_WORDS) || hasWord(text, DIESEL_WORDS) || hasWord(text, LPG_WORDS)
    var lastFuels: Set<FuelFamily> = emptySet()
    for (c in clauses) {
        val fuels = buildSet {
            if (hasWord(c, GASOLINE_WORDS)) add(FuelFamily.GASOLINE)
            if (hasWord(c, DIESEL_WORDS)) add(FuelFamily.DIESEL)
            if (hasWord(c, LPG_WORDS)) add(FuelFamily.LPG)
        }
        val applies = when {
            fuels.isNotEmpty() -> family in fuels
            !anySpecific -> hasWord(text, GENERIC_WORDS)
            else -> family in lastFuels // «O gasóleo vai subir, mas depois desce» → sigue el último nombrado
        }
        if (fuels.isNotEmpty()) lastFuels = fuels
        if (!applies) continue
        val u = hasWord(c, UP_WORDS)
        val d = hasWord(c, DOWN_WORDS)
        if (u && !d) up++
        if (d && !u) down++
    }
    return when {
        up > down -> PriceDirection.UP
        down > up -> PriceDirection.DOWN
        else -> null
    }
}

/** Solo cuentan las noticias de los últimos [maxAgeDays] días (las previsiones caducan cada lunes). */
fun mondayVerdict(
    news: List<NewsItem>,
    family: FuelFamily,
    now: Long = System.currentTimeMillis(),
    maxAgeDays: Int = 7,
): MondayVerdict {
    val recent = news.filter { n -> n.publishedAt?.let { now - it <= maxAgeDays * 86_400_000L } ?: true }
        .sortedByDescending { it.publishedAt ?: 0L }
    val signals = recent.mapNotNull { n -> headlineDirection(n.title, family)?.let { n to it } }
    val up = signals.count { it.second == PriceDirection.UP }
    val down = signals.count { it.second == PriceDirection.DOWN }
    val kind = when {
        up > down -> MondayKind.FILL_NOW
        down > up -> MondayKind.WAIT
        else -> MondayKind.UNCLEAR
    }
    val evidence = when (kind) {
        MondayKind.FILL_NOW -> signals.firstOrNull { it.second == PriceDirection.UP }?.first
        MondayKind.WAIT -> signals.firstOrNull { it.second == PriceDirection.DOWN }?.first
        MondayKind.UNCLEAR -> null
    }
    return MondayVerdict(kind, evidence, up, down)
}

/** Días que faltan hasta el próximo lunes (1 el domingo, 7 el lunes). */
fun daysToMonday(today: LocalDate = LocalDate.now()): Int {
    val d = DayOfWeek.MONDAY.value - today.dayOfWeek.value
    return if (d <= 0) d + 7 else d
}
