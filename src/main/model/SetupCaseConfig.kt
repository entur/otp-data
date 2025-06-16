package org.entur.otp.setup.model

import org.entur.otp.setup.framework.toStr

data class SetupCaseConfig(
    val path: String,
    val osm: List<String>,
    val geojson: List<String>,
    val netex: List<String>
) : Comparable<SetupCaseConfig> {

  fun description() : String {
    return "{Path: $path, OSM: ${osm.toStr()}, GeoJson: ${geojson.toStr()}, NeTEx: ${netex.toStr()}}"
  }

  override fun toString(): String {
    return description()
  }

  override fun compareTo(other: SetupCaseConfig): Int {
    return path.compareTo(other.path)
  }
}