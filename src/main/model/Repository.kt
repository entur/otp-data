package org.entur.otp.setup.model

import java.io.File


class Repository(val config: Config) {

  fun findSetupCase(path: String, env: Map<String, String> = mapOf()): SetupCase {
    val setupCase = config.cases.find { it.path == path }
    if (setupCase == null) {
      throw Exception("Setup case not found: '$path'")
    }
    val osm = mapToWebResource("osm", setupCase.osm, config.osm, env)
    val transit = mapToWebResource("transit", setupCase.netex, config.netex, env)
    val geojson = mapToFileResource("geojson", setupCase.geojson)

    return SetupCase(path, osm, geojson, transit)
  }

  private fun mapToWebResource(
      type: String,
      ids: List<String>,
      c: List<ConfResource>,
      env: Map<String, String>
  ): List<WebResource> {
    val result: ArrayList<WebResource> = arrayListOf()

    for (id in ids) {
      val r = c.map { it.find(id, env) }.filter { it != null }
      if (r.isEmpty()) {
        throw Exception("Error! No $type configuration found for id: '$id'")
      }
      r.first()?.let { result.add(it) }
    }
    return result
  }

  private fun mapToFileResource(
      type: String,
      ids: List<String>
  ) : List<FileResource> {
    val result: ArrayList<FileResource> = arrayListOf()
    for (id in ids) {
      result.add(
          when (type) {
            "geojson" -> FileResource(id, type, File(type))
            else -> throw IllegalArgumentException("Unknown type: $type")
          }
      )
    }
    return result
  }
}