package com.chungaro.combustible.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.ui.Atesta

/** Bloque completo para pedir la ubicación. */
@Composable
internal fun PermissionCard(onRequest: () -> Unit) {
    SectionCard(
        icon = Icons.Outlined.LocationOn,
        accent = Atesta.colors.accent,
        title = "Ativa a localização",
        subtitle = "Para veres as bombas perto de ti",
    ) {
        LocationPrompt(onRequest)
    }
}

/** Texto y botón para pedir la ubicación, dentro de una sección. */
@Composable
internal fun LocationPrompt(onRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Com a tua localização mostramos como mudam os preços das bombas da tua zona e onde fica a mais barata.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Usar a minha localização")
        }
    }
}
