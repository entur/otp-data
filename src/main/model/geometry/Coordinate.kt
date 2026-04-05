package org.entur.otp.setup.model.geometry

class Coordinate(val lat: Double, val lon: Double) {

  override fun toString(): String = "($lat, $lon)"
  operator fun minus(p: Coordinate) = Coordinate(lat - p.lat, lon - p.lon)
}
