package org.entur.otp.setup.model.geometry

class Polygon(val coordinates: List<Coordinate>) {
  private val box = BBox.instance.bbox(coordinates)

  /**
   * Ray-casting point-in-polygon test.
   * Returns true if (lon, lat) is inside the polygon ring.
   */
  fun contains(p: Coordinate): Boolean {
    if(!box.inside(p)) {
      return false
    }
    var inside = false
    var j = coordinates.size - 1
    for (i in coordinates.indices) {
      val s = coordinates[i]
      val t = coordinates[j]
      val d = t - s

      if ((s.lon > p.lon) != (t.lon > p.lon) && p.lat < d.lat * (p.lon - s.lon) / d.lon + s.lat) {
        inside = !inside
      }
      j = i
    }
    return inside
  }

  override fun toString(): String {
    return "Polygon$coordinates"
  }
}