package org.entur.otp.setup.framework

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.entur.otp.setup.model.geometry.Coordinate
import org.entur.otp.setup.model.geometry.Polygon
import java.io.File


/**
 * Parse the first Polygon geometry found in a GeoJSON FeatureCollection.
 * Coordinates are expected in [longitude, latitude] order (standard GeoJSON).
 */
fun parseGeoJsonPolygon(file: File): Polygon {
  return parseGeoJsonPolygon(ObjectMapper().readTree(file))
}

/**
 * Parse the first Polygon geometry found in a GeoJSON FeatureCollection.
 * Coordinates are expected in [longitude, latitude] order (standard GeoJSON).
 */
fun parseGeoJsonPolygon(root: JsonNode): Polygon {
  val coordinates = root
      .path("features").first()
      .path("geometry")
      .path("coordinates")
      .first()
  return Polygon(coordinates.map { coord ->
    Coordinate(coord[1].asDouble(), coord[0].asDouble())
  })
}

