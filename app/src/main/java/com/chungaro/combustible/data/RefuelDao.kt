package com.chungaro.combustible.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RefuelDao {
    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY epochDay ASC, odometer ASC, id ASC")
    abstract fun observeForVehicle(vehicleId: Long): Flow<List<Refuel>>

    @Query("SELECT * FROM refuels WHERE vehicleId = :vehicleId ORDER BY epochDay ASC, odometer ASC, id ASC")
    abstract suspend fun forVehicle(vehicleId: Long): List<Refuel>

    @Upsert
    abstract suspend fun upsert(refuel: Refuel)

    @Delete
    abstract suspend fun delete(refuel: Refuel)

    // --- Vehículos ---
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    abstract fun observeVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    abstract suspend fun vehicles(): List<Vehicle>

    @Upsert
    abstract suspend fun upsertVehicle(vehicle: Vehicle): Long

    @Query("DELETE FROM vehicles WHERE id = :id")
    abstract suspend fun deleteVehicleRow(id: Long)

    @Query("DELETE FROM refuels WHERE vehicleId = :id")
    abstract suspend fun deleteRefuelsOf(id: Long)

    @Query("DELETE FROM reminders WHERE vehicleId = :id")
    abstract suspend fun deleteRemindersOf(id: Long)

    @Transaction
    open suspend fun deleteVehicle(id: Long) {
        deleteRefuelsOf(id)
        deleteRemindersOf(id)
        deleteVehicleRow(id)
    }

    // --- Recordatorios ---
    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY done ASC, id DESC")
    abstract fun observeReminders(vehicleId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE done = 0")
    abstract suspend fun pendingReminders(): List<Reminder>

    @Upsert
    abstract suspend fun upsertReminder(reminder: Reminder)

    @Delete
    abstract suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT MAX(odometer) FROM refuels WHERE vehicleId = :vehicleId")
    abstract suspend fun maxOdometer(vehicleId: Long): Double?

    // --- Copia de seguridad ---
    @Query("SELECT * FROM refuels")
    abstract suspend fun allRefuels(): List<Refuel>

    @Query("SELECT * FROM reminders")
    abstract suspend fun allReminders(): List<Reminder>

    @Query("DELETE FROM refuels")
    abstract suspend fun clearRefuels()

    @Query("DELETE FROM reminders")
    abstract suspend fun clearReminders()

    @Query("DELETE FROM vehicles")
    abstract suspend fun clearVehicles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVehicles(list: List<Vehicle>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRefuels(list: List<Refuel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReminders(list: List<Reminder>)

    @Transaction
    open suspend fun replaceAll(vehicles: List<Vehicle>, refuels: List<Refuel>, reminders: List<Reminder>) {
        clearRefuels()
        clearReminders()
        clearVehicles()
        insertVehicles(vehicles)
        insertRefuels(refuels)
        insertReminders(reminders)
    }
}
