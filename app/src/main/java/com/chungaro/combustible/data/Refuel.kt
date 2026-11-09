package com.chungaro.combustible.data

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
)

fun Refuel.pricePerUnit(): Double = if (quantity > 0) totalCost / quantity else 0.0
