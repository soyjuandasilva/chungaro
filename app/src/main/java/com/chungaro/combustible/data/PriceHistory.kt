package com.chungaro.combustible.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Precio de un combustible en un día. La DGEG no publica histórico, así que
 * la app lo va guardando cada vez que descarga precios.
 * stationId = ZONE_AVG / ZONE_MIN para la media y el mínimo de la zona.
 */
@Entity(tableName = "price_history", primaryKeys = ["day", "fuelId", "stationId"])
data class PricePoint(
    val day: Long,
    val fuelId: Int,
    val stationId: Long,
    val price: Double,
)

@Dao
abstract class HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(points: List<PricePoint>)

    @Query("SELECT * FROM price_history WHERE fuelId = :fuelId AND stationId = :stationId AND day >= :fromDay ORDER BY day ASC")
    abstract fun observe(fuelId: Int, stationId: Long, fromDay: Long): Flow<List<PricePoint>>

    @Query("DELETE FROM price_history WHERE day < :beforeDay")
    abstract suspend fun prune(beforeDay: Long)
}
