package config.model

data class SetupCase(
    val name: String,
    val osm: List<WebResource>,
    val geojson: List<String>,
    val netex: List<WebResource>
) {
  override fun toString(): String {
    return "Case(\n  name='$name', \n  osm=$osm, \n  geojson=$geojson, \n  transit=$netex\n)"
  }
}