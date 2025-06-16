package org.entur.otp.setup.csv

import org.entur.otp.setup.model.SetupCaseConfig
import org.entur.otp.setup.framework.splt
import java.io.File

object SetupCaseParser {
  val HEADERS = listOf("NAME", "OSM", "GEOJSON", "TRANSIT FEEDS")

  fun parse() : List<SetupCaseConfig> {
    val result = ArrayList<SetupCaseConfig>()
    val reader = File("cases.txt")

    for (it in reader.readLines()) {
      val line = it.trim()
      val isHeader = HEADERS.all { line.contains(it, true) }

      if (isHeader || line.isBlank() || line.startsWith("#")) {
        continue;
      }
      result.add(parseLine(line))
    }
    return result
  }

  fun parseLine(line : String) :  SetupCaseConfig {
    val args = line.split('|').map { it.trim() }
    return SetupCaseConfig(args[0], args[1].splt(), args[2].splt(), args[3].splt())
  }
}