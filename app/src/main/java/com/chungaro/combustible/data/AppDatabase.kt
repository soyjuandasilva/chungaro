package com.chungaro.combustible.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PricePoint::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Tabla de vehículos tal como era en la versión 2 (la 2->3 añade columnas). */
        private const val CREATE_VEHICLES =
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `fuelTypeId` INTEGER NOT NULL, `tankLiters` REAL NOT NULL, `consumption` REAL NOT NULL)"
        private const val CREATE_REMINDERS =
            "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`vehicleId` INTEGER NOT NULL, `title` TEXT NOT NULL, `dueEpochDay` INTEGER, `dueOdometer` REAL, " +
                "`done` INTEGER NOT NULL, `notes` TEXT NOT NULL)"
        const val DEFAULT_VEHICLE_SQL =
            "INSERT OR IGNORE INTO `vehicles` (`id`, `name`, `fuelTypeId`, `tankLiters`, `consumption`) " +
                "VALUES (1, 'O meu carro', 3201, 50.0, 6.5)"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_VEHICLES)
                db.execSQL(CREATE_REMINDERS)
                db.execSQL(DEFAULT_VEHICLE_SQL)
                db.execSQL("ALTER TABLE `refuels` ADD COLUMN `vehicleId` INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** v2 -> v3: tipo, marca, modelo y año del vehículo. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'car'")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `brand` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `model` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicles` ADD COLUMN `year` INTEGER")
            }
        }

        /** v3 -> v4: histórico de precios. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `price_history` (`day` INTEGER NOT NULL, `fuelId` INTEGER NOT NULL, " +
                        "`stationId` INTEGER NOT NULL, `price` REAL NOT NULL, PRIMARY KEY(`day`, `fuelId`, `stationId`))",
                )
            }
        }

        /**
         * v4 -> v5: se quita «O meu veículo». Antes de borrar sus tablas, el
         * combustible del vehículo elegido pasa a ser el combustible habitual.
         */
        fun migration4to5(context: Context) = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val store = SettingsStore(context)
                if (!store.hasFuel()) {
                    runCatching {
                        db.query("SELECT `fuelTypeId` FROM `vehicles` WHERE `id` = ?", arrayOf<Any>(store.legacySelectedVehicle())).use { c ->
                            if (c.moveToFirst()) store.saveFuel(c.getInt(0))
                        }
                    }
                }
                store.clearLegacyVehicleSettings()
                db.execSQL("DROP TABLE IF EXISTS `refuels`")
                db.execSQL("DROP TABLE IF EXISTS `vehicles`")
                db.execSQL("DROP TABLE IF EXISTS `reminders`")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chungaro.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, migration4to5(context.applicationContext))
                    .build().also { instance = it }
            }
    }
}
