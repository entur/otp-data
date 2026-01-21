package org.entur.otp.setup.model

import java.io.File


val CONFIG_ROOT_DIR = File("config")

data class Config(
    val osm: List<ConfResource>,
    val netex: List<ConfResource>,
    val cases: List<SetupCaseConfig>,
    val env: Map<String, String>
) {

  override fun toString(): String {
    return """
      |Config(
      |  osm=${osm.map { "\n    $it" }},
      |  netex=${netex.map { "\n    $it" }},
      |  cases=${cases.map { "\n    $it" }}
      |)
      """.trimMargin("|")
  }
}