package com.chungaro.combustible.data

import org.json.JSONArray
import org.json.JSONObject

/** Copia de seguridad en JSON de vehículos, cargas y recordatorios. */
object Backup {
    private const val VERSION = 1

    fun toJson(vehicles: List<Vehicle>, refuels: List<Refuel>, reminders: List<Reminder>): String {
        val root = JSONObject()
        root.put("app", "chungaro")
        root.put("version", VERSION)
        root.put("vehicles", JSONArray().apply {
            vehicles.forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("fuelTypeId", it.fuelTypeId)
                    .put("tankLiters", it.tankLiters).put("consumption", it.consumption)
                    .put("type", it.type).put("brand", it.brand).put("model", it.model)
                    .put("year", it.year ?: JSONObject.NULL))
            }
        })
        root.put("refuels", JSONArray().apply {
            refuels.forEach {
                put(JSONObject().put("id", it.id).put("epochDay", it.epochDay).put("odometer", it.odometer)
                    .put("quantity", it.quantity).put("totalCost", it.totalCost).put("fullTank", it.fullTank)
                    .put("notes", it.notes).put("vehicleId", it.vehicleId))
            }
        })
        root.put("reminders", JSONArray().apply {
            reminders.forEach {
                put(JSONObject().put("id", it.id).put("vehicleId", it.vehicleId).put("title", it.title)
                    .put("dueEpochDay", it.dueEpochDay ?: JSONObject.NULL)
                    .put("dueOdometer", it.dueOdometer ?: JSONObject.NULL)
                    .put("done", it.done).put("notes", it.notes))
            }
        })
        return root.toString(2)
    }

    data class Content(val vehicles: List<Vehicle>, val refuels: List<Refuel>, val reminders: List<Reminder>)

    fun fromJson(json: String): Content {
        val root = JSONObject(json)
        require(root.optString("app") == "chungaro") { "O ficheiro não é uma cópia de segurança desta app" }
        val vehicles = root.optJSONArray("vehicles").objects().map {
            Vehicle(
                id = it.getLong("id"),
                name = it.optString("name", "O meu carro"),
                fuelTypeId = it.optInt("fuelTypeId", 3201),
                tankLiters = it.optDouble("tankLiters", 50.0),
                consumption = it.optDouble("consumption", 6.5),
                type = it.optString("type", "car"),
                brand = it.optString("brand", ""),
                model = it.optString("model", ""),
                year = if (it.isNull("year") || !it.has("year")) null else it.getInt("year"),
            )
        }.ifEmpty { listOf(Vehicle(1, "O meu carro", 3201, 50.0, 6.5)) }
        val refuels = root.optJSONArray("refuels").objects().map {
            Refuel(
                id = it.getLong("id"),
                epochDay = it.getLong("epochDay"),
                odometer = it.getDouble("odometer"),
                quantity = it.getDouble("quantity"),
                totalCost = it.getDouble("totalCost"),
                fullTank = it.optBoolean("fullTank", true),
                notes = it.optString("notes", ""),
                vehicleId = it.optLong("vehicleId", 1),
            )
        }
        val reminders = root.optJSONArray("reminders").objects().map {
            Reminder(
                id = it.getLong("id"),
                vehicleId = it.optLong("vehicleId", 1),
                title = it.optString("title", ""),
                dueEpochDay = if (it.isNull("dueEpochDay")) null else it.getLong("dueEpochDay"),
                dueOdometer = if (it.isNull("dueOdometer")) null else it.getDouble("dueOdometer"),
                done = it.optBoolean("done", false),
                notes = it.optString("notes", ""),
            )
        }
        return Content(vehicles, refuels, reminders)
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
