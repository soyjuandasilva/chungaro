package com.chungaro.combustible.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Refuel::class, Vehicle::class, Reminder::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun refuelDao(): RefuelDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private const val CREATE_VEHICLES =
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `fuelTypeId` INTEGER NOT NULL, `tankLiters` REAL NOT NULL, `consumption` REAL NOT NULL)"
        private const val CREATE_REMINDERS =
            "CREATE TABLE IF NOT EXISTS `reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`vehicleId` INTEGER NOT NULL, `title` TEXT NOT NULL, `dueEpochDay` INTEGER, `dueOdometer` REAL, " +
                "`done` INTEGER NOT NULL, `notes` TEXT NOT NULL)"
        const val DEFAULT_VEHICLE_SQL =
            "INSERT OR IGNORE INTO `vehicles` (`id`, `name`, `fuelTypeId`, `tankLiters`, `consumption`) " +
                "VALUES (1, 'Mi coche', 3201, 50.0, 6.5)"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_VEHICLES)
                db.execSQL(CREATE_REMINDERS)
                db.execSQL(DEFAULT_VEHICLE_SQL)
                db.execSQL("ALTER TABLE `refuels` ADD COLUMN `vehicleId` INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chungaro.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(DEFAULT_VEHICLE_SQL)
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
