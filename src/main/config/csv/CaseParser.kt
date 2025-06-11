package config.csv

import org.entur.otp.data.config.model.ConfCase
import org.entur.otp.data.config.model.splt
import java.io.File

object CaseParser {
  val HEADERS = listOf("NAME", "OSM", "GEOJSON", "TRANSIT FEEDS")

  fun parse() : List<ConfCase> {
    val result = ArrayList<ConfCase>()
    val reader = File("cases.txt")

    for (it in reader.readLines()) {
      val line = it.trim()
      val header = HEADERS.all { line.contains(it, true) }

      if (header || line.isBlank() || line.startsWith("#")) {
        continue;
      }
      result.add(parseLine(line))
    }
    return result
  }

  fun parseLine(line : String) : ConfCase {
    val args = line.split('|').map { it.trim() }
    return ConfCase(args[0], args[1].splt(), args[2].splt(), args[3].splt())
  }
}