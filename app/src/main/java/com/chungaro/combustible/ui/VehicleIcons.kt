package com.chungaro.combustible.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.RvHookup
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector
import com.chungaro.combustible.data.Vehicle

/** Icono de cada tipo de vehículo (en lugar de emojis, que cambian según el móvil). */
fun vehicleTypeIcon(typeId: String): ImageVector = when (typeId) {
    "moto" -> Icons.Outlined.TwoWheeler
    "van" -> Icons.Outlined.AirportShuttle
    "truck" -> Icons.Outlined.LocalShipping
    "camper" -> Icons.Outlined.RvHookup
    else -> Icons.Outlined.DirectionsCar
}

val Vehicle.icon: ImageVector get() = vehicleTypeIcon(type)
