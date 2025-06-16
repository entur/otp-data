package org.entur.otp.setup.model

import org.entur.otp.setup.model.ConfResource
import org.entur.otp.setup.model.Config

class Repository(val config: Config) {

  fun findSetupCase(path: String, env: Map<String, String> = mapOf()): SetupCase {
    val setupCase = config.cases.find { it.path == path }
    if (setupCase == null) {
      throw Exception("Setup case not found: '$path'")
    }
    val osm = mapResource("osm", setupCase.osm, config.osm, env)
    val transit = mapResource("transit", setupCase.netex, config.netex, env)

    return SetupCase(path, osm, setupCase.geojson, transit)
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