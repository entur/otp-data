package org.entur.otp.setup.model.geometry

class BBox(val min: Coordinate, val max: Coordinate) {



  fun inside(c: Coordinate) = min.lat <= c.lat && c.lat <= max.lat && min.lon <= c.lon && c.lon <= max.lon

  object instance {
    fun bbox(coordinates: List<Coordinate>): BBox {
      var latMin = coordinates[0].lat
      var lonMin = coordinates[0].lon
      var latMax = coordinates[0].lat
      var lonMax = coordinates[0].lon

      for (c in coordinates) {
        if (c.lat < latMin) {
          latMin = c.lat
        }
        else if (c.lat > latMax) {
          latMax = c.lat
        }
        if (c.lon < lonMin) {
          lonMin = c.lon
        }
        else if (c.lon > lonMax) {
          lonMax = c.lon
        }
      }
      return BBox(Coordinate(latMin, lonMin), Coordinate(latMax, lonMax))
    }
  }
}

