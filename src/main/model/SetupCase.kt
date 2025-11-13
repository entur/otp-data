package org.entur.otp.setup.model

data class SetupCase(
    /**
     * The relative path to use as the target location for the setup case. This is also used
     * as the unique id for the setup case.
     */
    val path: String,
    val osm: List<WebResource>,
    val geojson: List<FileResource>,
    val netex: List<WebResource>
) {
  override fun toString(): String {
    return "SetupCase(\n  path='$path', \n  osm=$osm, \n  geojson=${geojson}, \n  transit=$netex\n)"
  }
}
