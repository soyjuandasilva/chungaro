package com.chungaro.combustible.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RefuelDao {
    @Query("SELECT * FROM refuels ORDER BY epochDay ASC, odometer ASC, id ASC")
    fun observeAll(): Flow<List<Refuel>>

    @Upsert
    suspend fun upsert(refuel: Refuel)

    @Delete
    suspend fun delete(refuel: Refuel)
}
