package com.example.format

import com.example.model.GpsPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale

object KmlHandler {

    fun parseKml(inputStream: InputStream): Pair<String, List<GpsPoint>> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val points = mutableListOf<GpsPoint>()
        var routeName = "Ruta KML Importada"
        var inCoordinates = false
        var inName = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase(Locale.ROOT)
                    currentTag = tag
                    if (tag == "coordinates") {
                        inCoordinates = true
                    } else if (tag == "name" && routeName == "Ruta KML Importada") {
                        inName = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (inName && text.isNotEmpty() && routeName == "Ruta KML Importada") {
                        routeName = text
                    } else if (inCoordinates && text.isNotEmpty()) {
                        // KML coordinates are typically: lon,lat,alt lon,lat,alt ...
                        val tuples = text.split("\\s+".toRegex())
                        for (tuple in tuples) {
                            val parts = tuple.split(",")
                            if (parts.size >= 2) {
                                val lon = parts[0].trim().toDoubleOrNull()
                                val lat = parts[1].trim().toDoubleOrNull()
                                val alt = if (parts.size >= 3) parts[2].trim().toDoubleOrNull() ?: 0.0 else 0.0
                                if (lat != null && lon != null) {
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
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase(Locale.ROOT)
                    if (tag == "coordinates") inCoordinates = false
                    if (tag == "name") inName = false
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return Pair(routeName, points)
    }
}
