package com.chungaro.combustible.nearby

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.news.NewsItem

/** "Preço dos combustíveis na próxima semana": las 5 últimas noticias de Google News Portugal. */
@Composable
fun NewsCard(vm: NearbyViewModel, myFuelId: Int) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.loadNews() }
    SectionCard(
        icon = "📰",
        accent = SectionColors.Amber,
        title = "Próxima semana nas notícias",
        subtitle = "Preço dos combustíveis na próxima semana",
        footnote = "Via Google Notícias. Os textos pertencem aos respetivos meios.",
    ) {
        MondayCard(vm, FUEL_TYPES.firstOrNull { it.id == myFuelId } ?: FUEL_TYPES.first())
        when {
            vm.news.isEmpty() && vm.newsLoading -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
            vm.news.isEmpty() -> Column {
                Text(
                    if (vm.newsError) "Não conseguimos carregar as notícias. Verifica a ligação." else "Sem notícias de momento.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { vm.loadNews(force = true) }) { Text("Tentar outra vez") }
            }
            else -> vm.news.forEach { item -> NewsRow(item) { openLink(context, item.link) } }
        }
    }
}

@Composable
private fun NewsRow(item: NewsItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (item.snippet.isNotBlank()) {
                Text(item.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                listOfNotNull(item.source.ifBlank { null }, item.publishedAt?.let { ago(it) }).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SectionColors.Amber,
            )
        }
    }
}

/** "há 3 h", "há 2 dias". */
private fun ago(time: Long): String {
    val min = ((System.currentTimeMillis() - time) / 60_000L).coerceAtLeast(0)
    return when {
        min < 60 -> "há $min min"
        min < 24 * 60 -> "há ${min / 60} h"
        min < 48 * 60 -> "ontem"
        else -> "há ${min / (24 * 60)} dias"
    }
}

internal fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Sin navegador: no hay nada que abrir.
    }
}
