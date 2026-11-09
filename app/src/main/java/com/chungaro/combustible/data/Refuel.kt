package com.chungaro.combustible.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Un abastecimiento de combustible. */
@Entity(tableName = "refuels")
data class Refuel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Fecha como día desde 1970-01-01 (LocalDate.toEpochDay). */
    val epochDay: Long,
    /** Lectura del odómetro en km. */
    val odometer: Double,
    /** Cantidad cargada, en la unidad elegida (litros o galones). */
    val quantity: Double,
    /** Total pagado. */
    val totalCost: Double,
    /** Si se llenó el tanque; necesario para calcular el rendimiento. */
    val fullTank: Boolean = true,
    val notes: String = "",
    @ColumnInfo(defaultValue = "1") val vehicleId: Long = 1,
)

fun Refuel.pricePerUnit(): Double = if (quantity > 0) totalCost / quantity else 0.0

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Id de combustible de la DGEG (ver FUEL_TYPES). */
    val fuelTypeId: Int,
    val tankLiters: Double,
    /** Consumo estimado en L/100 km, usado mientras no haya historial. */
    val consumption: Double,
)

/** Recordatorio de mantenimiento: vence por fecha, por kilometraje o por ambos. */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val dueEpochDay: Long? = null,
    val dueOdometer: Double? = null,
    val done: Boolean = false,
    val notes: String = "",
)
