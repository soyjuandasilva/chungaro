package com.chungaro.combustible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    @Test
    fun roundTrip() {
        val vehicles = listOf(Vehicle(1, "Coche", 3201, 50.0, 6.5), Vehicle(2, "Moto", 3400, 15.0, 4.0, "moto", "Honda", "PCX 125", 2021))
        val refuels = listOf(Refuel(5, 20000, 1234.5, 40.0, 70.0, false, "nota", 2))
        val reminders = listOf(
            Reminder(3, 1, "Cambio de aceite", null, 15000.0, false, ""),
            Reminder(4, 2, "Seguro", 20500, null, true, "x"),
        )
        val c = Backup.fromJson(Backup.toJson(vehicles, refuels, reminders))
        assertEquals(vehicles, c.vehicles)
        assertEquals(refuels, c.refuels)
        assertEquals(reminders, c.reminders)
        assertNull(c.reminders[0].dueEpochDay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherFiles() {
        Backup.fromJson("""{"hola":1}""")
    }
}
