package config.model

import org.entur.otp.data.config.model.ConfResource
import org.entur.otp.data.config.model.Config

class Repository(val config: Config) {

  fun getCase(name: String, env: Map<String, String> = mapOf()): Case {
    val confCase = config.cases.find { it.name == name }
    if (confCase == null) {
      throw Exception("Configuration case not found: '$name'")
    }
    val osm = mapResource("osm", confCase.osm, config.osm, env)
    val transit = mapResource("transit", confCase.netex, config.netex, env)

    return Case(name, osm, confCase.geojson, transit)
  }

  private fun mapResource(
      type: String,
      ids: List<String>,
      c: List<ConfResource>,
      env: Map<String, String>
  ): List<WebResource> {
    val result: ArrayList<WebResource> = arrayListOf()

    for (id in ids) {
      val r = c.map { it.find(id, env) }.filter { it != null }
      if (r.isEmpty()) {
        throw Exception("Error! No ${type} configuration found for id: '$id'")
      }
      r.first()?.let { result.add(it) }
    }
    return result
  }
}