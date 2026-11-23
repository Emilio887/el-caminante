package com.example.format

import com.example.model.GpsPoint
import com.example.model.Route
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxHandler {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Exports a route or recorded points into standard GPX 1.1 format XML string.
     */
    fun exportToGpx(title: String, description: String, points: List<GpsPoint>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Sendero MTB Android\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapeXml(title)).append("</name>\n")
        sb.append("    <desc>").append(escapeXml(description)).append("</desc>\n")
        sb.append("    <time>").append(isoFormat.format(Date())).append("</time>\n")
        sb.append("  </metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapeXml(title)).append("</name>\n")
        sb.append("    <trkseg>\n")

        for (pt in points) {
            sb.append("      <trkpt lat=\"").append(pt.latitude).append("\" lon=\"").append(pt.longitude).append("\">\n")
            sb.append("        <ele>").append(String.format(Locale.US, "%.1f", pt.altitude)).append("</ele>\n")
            sb.append("        <time>").append(isoFormat.format(Date(pt.timestamp))).append("</time>\n")
            if (pt.speed > 0) {
                sb.append("        <speed>").append(String.format(Locale.US, "%.2f", pt.speed)).append("</speed>\n")
            }
            if (pt.heartRate != null || pt.cadence != null || pt.power != null) {
                sb.append("        <extensions>\n")
                pt.heartRate?.let { sb.append("          <hr>").append(it).append("</hr>\n") }
                pt.cadence?.let { sb.append("          <cad>").append(it).append("</cad>\n") }
                pt.power?.let { sb.append("          <power>").append(it).append("</power>\n") }
                sb.append("        </extensions>\n")
            }
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>")
        return sb.toString()
    }

    /**
     * Parses a GPX stream and extracts GpsPoints and track name.
     */
    fun parseGpx(inputStream: InputStream): Pair<String, List<GpsPoint>> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val points = mutableListOf<GpsPoint>()
        var trackName = "Ruta GPX Importada"
        var currentLat = 0.0
        var currentLon = 0.0
        var currentEle = 0.0
        var currentSpeed = 0f
        var currentTime = System.currentTimeMillis()
        var currentTag = ""
        var inTrkpt = false
        var inName = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase(Locale.ROOT)
                    currentTag = tag
                    if (tag == "trkpt" || tag == "wpt" || tag == "rtept") {
                        inTrkpt = true
                        currentLat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                        currentLon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                        currentEle = 0.0
                        currentSpeed = 0f
                        currentTime = System.currentTimeMillis()
                    } else if (tag == "name" && !inTrkpt) {
                        inName = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        if (inName && trackName == "Ruta GPX Importada") {
                            trackName = text
                        } else if (inTrkpt) {
                            when (currentTag) {
                                "ele" -> currentEle = text.toDoubleOrNull() ?: currentEle
                                "speed" -> currentSpeed = text.toFloatOrNull() ?: currentSpeed
                                "time" -> {
                                    try {
                                        currentTime = isoFormat.parse(text)?.time ?: currentTime
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase(Locale.ROOT)
                    if (tag == "trkpt" || tag == "wpt" || tag == "rtept") {
                        if (currentLat != 0.0 && currentLon != 0.0) {
                            points.add(
                                GpsPoint(
                                    latitude = currentLat,
                                    longitude = currentLon,
                                    altitude = currentEle,
                                    speed = currentSpeed,
                                    timestamp = currentTime
                                )
                            )
                        }
                        inTrkpt = false
                    } else if (tag == "name") {
                        inName = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return Pair(trackName, points)
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
