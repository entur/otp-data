package config.model

import java.io.File

data class WebResource(val name : String, val url: String, val file: String) {
  fun fullUrl() : String = "$url/$file"

  fun asFile(dir : File) = File(dir, file)
  override fun toString(): String {
    return "$name $url/$file"
  }
}

data class Case(
    val name: String,
    val osm: List<WebResource>,
    val geojson: List<String>,
    val netex: List<WebResource>
) {
  override fun toString(): String {
    return "Case(\n  name='$name', \n  osm=$osm, \n  geojson=$geojson, \n  transit=$netex\n)"
  }
}
