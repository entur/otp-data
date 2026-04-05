package framework

import com.fasterxml.jackson.databind.ObjectMapper
import org.entur.otp.setup.framework.parseGeoJsonPolygon
import org.entur.otp.setup.model.geometry.Coordinate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ParseGeoJsonPolygonTest {
  @Test
  fun parseGeoJsonPolygon() {
    val root = ObjectMapper().readTree(
        """
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {},
      "geometry": {
        "coordinates": [
          [
            [
              9.79,
              58.96
            ],
            [
              10.03,
              58.95
            ],
            [
              9.90,
              60.05
            ],
            [
              9.79,
              58.96
            ]
          ]
        ],
        "type": "Polygon"
      }
    }
  ]
}
"""
    )
    val polygon = parseGeoJsonPolygon(root)

    assertEquals(
        "Polygon[(58.96, 9.79), (58.95, 10.03), (60.05, 9.9), (58.96, 9.79)]",
        polygon.toString()
    )

    assertTrue(polygon.contains(Coordinate(59.5, 9.9)))
  }
}