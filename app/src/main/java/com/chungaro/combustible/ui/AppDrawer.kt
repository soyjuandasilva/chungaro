package com.chungaro.combustible.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chungaro.combustible.R
import com.chungaro.combustible.data.Vehicle
import com.chungaro.combustible.data.emoji
import com.chungaro.combustible.nearby.FUEL_TYPES
import com.chungaro.combustible.nearby.FuelPill

/** Menú lateral: tu vehículo arriba y las secciones de la app. */
@Composable
fun AppDrawer(
    vehicle: Vehicle?,
    onVehicle: () -> Unit,
    onOverview: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(Modifier.statusBarsPadding().padding(vertical = 16.dp)) {
            // Cabecera: tu vehículo
            Surface(
                onClick = onVehicle,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            if (vehicle != null) {
                                Text(vehicle.emoji, style = MaterialTheme.typography.headlineMedium)
                            } else {
                                Icon(painterResource(R.drawable.ic_fuel), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FUEL_TYPES.firstOrNull { it.id == vehicle?.fuelTypeId }?.let { FuelPill(it, compact = true) }
                        Text(vehicle?.name ?: "O meu veículo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Ver o meu veículo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 16.dp))

            DrawerItem("🚗", "Meu veículo", "Preços, abastecimentos e manutenção", onVehicle)
            DrawerItem("📊", "Preços e notícias", "Preço médio, evolução e próxima semana", onOverview)
            DrawerItem("❤️", "Bombas favoritas", "Procurar por distrito e guardar", onFavorites)
            DrawerItem("🔔", "Alertas de preço", "Avisa-me quando o preço descer", onSettings)

            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            DrawerItem("⚙️", "Definições", null, onSettings)

            Spacer(Modifier.height(24.dp))
            Text(
                "Atesta · Preços oficiais da DGEG",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            )
        }
    }
}

@Composable
private fun DrawerItem(icon: String, title: String, subtitle: String?, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Text(icon, style = MaterialTheme.typography.titleLarge) },
        label = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        selected = false,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
