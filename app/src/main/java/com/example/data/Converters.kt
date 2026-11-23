package com.example.data

import androidx.room.TypeConverter
import com.example.model.ActivityType
import com.example.model.GpsPoint
import com.example.model.OfflineStatus
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = ActivityType.fromString(value)

    @TypeConverter
    fun fromOfflineStatus(value: OfflineStatus): String = value.name

    @TypeConverter
    fun toOfflineStatus(value: String): OfflineStatus = try {
        OfflineStatus.valueOf(value)
    } catch (_: Exception) {
        OfflineStatus.PENDING
    }

    @TypeConverter
    fun fromGpsPoints(points: List<GpsPoint>?): String {
        if (points.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("lat", p.latitude)
            obj.put("lon", p.longitude)
            obj.put("alt", p.altitude)
            obj.put("spd", p.speed.toDouble())
            obj.put("t", p.timestamp)
            obj.put("acc", p.accuracy.toDouble())
            obj.put("b", p.bearing.toDouble())
            p.heartRate?.let { obj.put("hr", it) }
            p.cadence?.let { obj.put("cad", it) }
            p.power?.let { obj.put("pwr", it) }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toGpsPoints(data: String?): List<GpsPoint> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val list = mutableListOf<GpsPoint>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pt = GpsPoint(
                    latitude = obj.getDouble("lat"),
                    longitude = obj.getDouble("lon"),
                    altitude = obj.optDouble("alt", 0.0),
                    speed = obj.optDouble("spd", 0.0).toFloat(),
                    timestamp = obj.optLong("t", System.currentTimeMillis()),
                    accuracy = obj.optDouble("acc", 0.0).toFloat(),
                    bearing = obj.optDouble("b", 0.0).toFloat(),
                    heartRate = if (obj.has("hr")) obj.getInt("hr") else null,
                    cadence = if (obj.has("cad")) obj.getInt("cad") else null,
                    power = if (obj.has("pwr")) obj.getInt("pwr") else null
                )
                list.add(pt)
            }
        } catch (_: Exception) {
        }
        return list
    }
}
