package com.example.format

import com.example.model.GpsPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

object GeoJsonHandler {

    fun parseGeoJson(inputStream: InputStream): Pair<String, List<GpsPoint>> {
        val reader = InputStreamReader(inputStream, "UTF-8")
        val content = reader.readText()
        val json = JSONObject(content)

        var routeName = "Ruta GeoJSON Importada"
        val points = mutableListOf<GpsPoint>()

        if (json.has("name")) {
            routeName = json.optString("name", routeName)
        }

        fun extractFromCoordinates(coordArray: JSONArray) {
            for (i in 0 until coordArray.length()) {
                val pt = coordArray.optJSONArray(i) ?: continue
                if (pt.length() >= 2) {
                    val lon = pt.optDouble(0)
                    val lat = pt.optDouble(1)
                    val alt = if (pt.length() >= 3) pt.optDouble(2, 0.0) else 0.0
                    points.add(
                        GpsPoint(
                            latitude = lat,
                            longitude = lon,
                            altitude = alt,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        val type = json.optString("type", "")
        when (type) {
            "FeatureCollection" -> {
                val features = json.optJSONArray("features")
                if (features != null) {
                    for (i in 0 until features.length()) {
                        val feat = features.optJSONObject(i) ?: continue
                        val props = feat.optJSONObject("properties")
                        if (props != null && props.has("name")) {
                            routeName = props.optString("name", routeName)
                        }
                        val geom = feat.optJSONObject("geometry")
                        if (geom != null) {
                            val geomType = geom.optString("type", "")
                            if (geomType == "LineString") {
                                extractFromCoordinates(geom.optJSONArray("coordinates") ?: JSONArray())
                            }
                        }
                    }
                }
            }
            "LineString" -> {
                extractFromCoordinates(json.optJSONArray("coordinates") ?: JSONArray())
            }
            "Feature" -> {
                val geom = json.optJSONObject("geometry")
                if (geom != null) {
                    extractFromCoordinates(geom.optJSONArray("coordinates") ?: JSONArray())
                }
            }
        }

        return Pair(routeName, points)
    }
}
