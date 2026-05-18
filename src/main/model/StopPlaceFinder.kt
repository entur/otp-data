package org.entur.otp.setup.model

import org.entur.otp.setup.model.geometry.Coordinate
import org.entur.otp.setup.model.geometry.Polygon
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory

data class StopFinderResult(val stopIds: Set<String>, val quayIds: Set<String>)

object StopPlaceFinder {

    /**
     * Scans all stops NeTEx XML files in [netexDir] (files matching "_stops_*.xml").
     * Returns IDs of StopPlaces whose centroid is inside [polygon], plus all Quay IDs
     * belonging to those StopPlaces.
     */
    fun findInPolygon(netexDir: File, polygon: Polygon): StopFinderResult {
        val stopFiles = netexDir.listFiles { f -> f.name.startsWith("_stops") && f.name.endsWith(".xml") }
            ?: return StopFinderResult(emptySet(), emptySet())

        val stopIds = mutableSetOf<String>()
        val quayIds = mutableSetOf<String>()

        for (file in stopFiles) {
            println("  Scanning stops file: ${file.name}")
            val handler = StopPlaceSaxHandler(polygon)
            SAXParserFactory.newInstance().newSAXParser().parse(file, handler)
            stopIds += handler.matchedStopIds
            quayIds += handler.matchedQuayIds
        }

        println("  Found ${stopIds.size} stop places and ${quayIds.size} quays inside polygon")
        return StopFinderResult(stopIds, quayIds)
    }
}

private class StopPlaceSaxHandler(private val polygon: Polygon) : DefaultHandler() {

    val matchedStopIds = mutableSetOf<String>()
    val matchedQuayIds = mutableSetOf<String>()

    // Current StopPlace state
    private var currentStopId: String? = null
    private var currentQuayIds = mutableListOf<String>()
    private var insideLocation = false
    private var insideLongitude = false
    private var insideLatitude = false
    private var currentLon: Double? = null
    private var currentLat: Double? = null
    private val charBuffer = StringBuilder()

    override fun startElement(uri: String?, localName: String?, qName: String?, attrs: Attributes?) {
        when (qName) {
            "StopPlace" -> {
                currentStopId = attrs?.getValue("id")
                currentQuayIds = mutableListOf()
                currentLon = null
                currentLat = null
            }
            "Quay" -> attrs?.getValue("id")?.let { currentQuayIds.add(it) }
            "Location" -> insideLocation = true
            "Longitude" -> if (insideLocation) { insideLongitude = true; charBuffer.clear() }
            "Latitude" -> if (insideLocation) { insideLatitude = true; charBuffer.clear() }
        }
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        if (insideLongitude || insideLatitude) {
            charBuffer.append(ch, start, length)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "Longitude" -> {
                if (insideLongitude) currentLon = charBuffer.toString().trim().toDoubleOrNull()
                insideLongitude = false
            }
            "Latitude" -> {
                if (insideLatitude) currentLat = charBuffer.toString().trim().toDoubleOrNull()
                insideLatitude = false
            }
            "Location" -> insideLocation = false
            "StopPlace" -> {
                val id = currentStopId ?: return
                val lon = currentLon ?: return
                val lat = currentLat ?: return
                if (polygon.contains(Coordinate(lat, lon))) {
                    matchedStopIds.add(id)
                    matchedQuayIds.addAll(currentQuayIds)
                }
                currentStopId = null
            }
        }
    }
}
