package com.chungaro.combustible.nearby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import java.util.Locale

/**
 * Insignia de marca: colores característicos y unas letras.
 * No son los logotipos oficiales (marcas registradas de sus dueños).
 */
data class BrandStyle(
    val label: String,
    val background: Long,
    val foreground: Long,
    val accent: Long? = null,
    /** Web oficial de la marca: de ahí se descarga su icono (logotipo). */
    val domain: String? = null,
    /** Logotipo incluido en la app (res/drawable-nodpi/brand_<logo>.webp). */
    val logo: String? = null,
)

/** Logotipos incluidos en la app, por la primera clave de cada marca. */
private val LOGOS = mapOf(
    "galp" to "galp", "repsol" to "repsol", "bp" to "bp", "prio" to "prio", "moeve" to "moeve", "cepsa" to "moeve",
    "shell" to "shell", "totalenergies" to "totalenergies", "intermarche" to "intermarche", "leclerc" to "leclerc",
    "auchan" to "auchan", "alves bandeira" to "alvesbandeira", "q8" to "q8", "esso" to "esso", "avia" to "avia",
    "petroprix" to "petroprix",
)

private data class BrandRule(val keys: List<String>, val style: BrandStyle)

private val RULES = listOf(
    BrandRule(listOf("galp", "petrogal"), BrandStyle("G", 0xFFFF6A13, 0xFFFFFFFF, domain = "galp.com")),
    BrandRule(listOf("repsol", "campsa", "petronor"), BrandStyle("R", 0xFFFF8200, 0xFFFFFFFF, 0xFFE4002B, domain = "repsol.pt")),
    BrandRule(listOf("bp"), BrandStyle("bp", 0xFF009B3A, 0xFFFFFFFF, 0xFFFFE600, domain = "bp.com")),
    BrandRule(listOf("prio"), BrandStyle("P", 0xFF5CB531, 0xFFFFFFFF, domain = "prio.pt")),
    BrandRule(listOf("moeve"), BrandStyle("M", 0xFF00A19A, 0xFFFFFFFF, domain = "moeveglobal.com")),
    BrandRule(listOf("cepsa"), BrandStyle("C", 0xFFE30613, 0xFFFFFFFF, domain = "cepsa.pt")),
    BrandRule(listOf("shell"), BrandStyle("S", 0xFFFFD500, 0xFFDD1D21, 0xFFDD1D21, domain = "shell.pt")),
    BrandRule(listOf("totalenergies", "total"), BrandStyle("T", 0xFFED0000, 0xFFFFFFFF, 0xFF0055A4, domain = "totalenergies.com")),
    BrandRule(listOf("intermarche", "ecomarche"), BrandStyle("I", 0xFFE2001A, 0xFFFFFFFF)),
    BrandRule(listOf("leclerc"), BrandStyle("E", 0xFF0066B3, 0xFFFFFFFF, 0xFFF39200, domain = "e-leclerc.pt")),
    BrandRule(listOf("auchan", "jumbo"), BrandStyle("A", 0xFFE2001A, 0xFFFFFFFF, 0xFF00A650, domain = "auchan.pt")),
    BrandRule(listOf("alves bandeira"), BrandStyle("AB", 0xFF003DA5, 0xFFFFFFFF, 0xFFFFCD00, domain = "alvesbandeira.pt")),
    BrandRule(listOf("oz energia", "oz"), BrandStyle("OZ", 0xFF1D70B7, 0xFFFFFFFF, domain = "ozenergia.pt")),
    BrandRule(listOf("avia"), BrandStyle("A", 0xFFE30613, 0xFFFFFFFF, 0xFF003B7A)),
    BrandRule(listOf("q8"), BrandStyle("Q8", 0xFF003B7A, 0xFFFFD200, domain = "q8.it")),
    BrandRule(listOf("esso"), BrandStyle("E", 0xFFFFFFFF, 0xFF0051A5, 0xFFE21B26, domain = "esso.com")),
    BrandRule(listOf("petroiberica"), BrandStyle("PI", 0xFF00539F, 0xFFFFFFFF, domain = "petroiberica.pt")),
    BrandRule(listOf("plenergy"), BrandStyle("PL", 0xFF7AB800, 0xFFFFFFFF, domain = "plenergy.es")),
    BrandRule(listOf("bxpress", "pingo doce"), BrandStyle("BX", 0xFF6CB33F, 0xFFFFFFFF)),
    BrandRule(listOf("mais combustiveis"), BrandStyle("+", 0xFF00843D, 0xFFFFFFFF)),
    BrandRule(listOf("petroprix"), BrandStyle("P", 0xFF2D3436, 0xFFFFFFFF)),
)

private val GENERIC = BrandStyle("⛽", 0xFF64748B, 0xFFFFFFFF)

/** Paleta para marcas desconocidas: el color sale del nombre (siempre el mismo). */
private val FALLBACK_COLORS = longArrayOf(
    0xFF0F766E, 0xFF1D4ED8, 0xFF7C3AED, 0xFFBE185D, 0xFFB45309, 0xFF047857, 0xFF4338CA, 0xFF9F1239,
)

private fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9+ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

fun brandStyle(brand: String): BrandStyle {
    val n = norm(brand)
    if (n.isBlank() || n == "generico" || n == "sem marca" || n == "outra" || n == "outro") return GENERIC
    val words = n.split(' ')
    RULES.forEach { rule ->
        if (rule.keys.any { key -> n == key || n.startsWith("$key ") || (" $key " in " $n ") || (key.length > 3 && n.contains(key)) }) {
            return rule.style.copy(logo = LOGOS[rule.keys.first()])
        }
    }
    val initials = words.filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(Locale.ROOT)
        .ifBlank { "?" }
    val color = FALLBACK_COLORS[(n.hashCode() and 0x7FFFFFFF) % FALLBACK_COLORS.size]
    return BrandStyle(initials, color, 0xFFFFFFFF)
}

/** URL del icono de la web oficial de la marca (servicio de iconos de Google). */
fun brandLogoUrl(domain: String): String = "https://www.google.com/s2/favicons?domain=$domain&sz=128"

/**
 * Insignia de la marca: muestra su logotipo (descargado de su web y guardado en
 * caché) y, si no hay o no carga, la insignia de colores con iniciales.
 */
@Composable
fun BrandBadge(brand: String, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val st = brandStyle(brand)
    // 1) Logotipo incluido en la app
    val ctx = LocalContext.current
    val localId = remember(st.logo) {
        st.logo?.let { ctx.resources.getIdentifier("brand_$it", "drawable", ctx.packageName) } ?: 0
    }
    if (localId != 0) {
        val painter = painterResource(localId)
        val ratio = painter.intrinsicSize.width / painter.intrinsicSize.height
        val wide = ratio > 2f
        Box(
            modifier
                .size(if (wide) size * 1.9f else size, size)
                .clip(if (wide) RoundedCornerShape(50) else CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0x22000000), if (wide) RoundedCornerShape(50) else CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painter,
                contentDescription = brand,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(if (wide) size * 0.14f else size * 0.16f),
            )
        }
        return
    }
    // 2) Icono de su web; 3) insignia de colores
    val domain = st.domain
    if (domain != null) {
        val context = LocalContext.current
        val request = remember(domain) {
            // Tamaño fijo: así la descarga empieza sin esperar a que se dibuje.
            ImageRequest.Builder(context).data(brandLogoUrl(domain)).size(128).build()
        }
        val painter = rememberAsyncImagePainter(request)
        if (painter.state is AsyncImagePainter.State.Success) {
            Box(
                modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0x22000000), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painter,
                    contentDescription = brand,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size * 0.66f),
                )
            }
            return
        }
    }
    InitialsBadge(st, size, modifier)
}

@Composable
private fun InitialsBadge(st: BrandStyle, size: Dp, modifier: Modifier) {
    val ring = st.accent?.let { Color(it) } ?: Color.White.copy(alpha = 0.0f)
    val fontSize = when (st.label.length) {
        1 -> size.value * 0.48f
        2 -> size.value * 0.36f
        else -> size.value * 0.28f
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(st.background))
            .border(width = if (st.accent != null) (size.value / 12).dp else 0.dp, color = ring, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            st.label,
            style = TextStyle(
                color = Color(st.foreground),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}
