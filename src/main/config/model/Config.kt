package org.entur.otp.data.config.model

import config.model.WebResource
import kotlin.text.split


data class Config(
    val osm: List<ConfResource>,
    val netex: List<ConfResource>,
    val cases: List<ConfCase>
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

data class ConfResource(val ids : List<String> = listOf(), val url: String, val file: String) {

  fun find(name : String, env : Map<String, String> = mapOf()) : WebResource? {
    if(!ids.contains(name)) {
      return null;
    }
    return WebResource(name, expand(url, "id", name, env), expand(file, "id", name, env))
  }
  override fun toString(): String {
    return "{\n\t\t\tids: ${ids.toStr()}\n\t\t\turl: ${url}\n\t\t\tfile: ${file}\n\t}"
  }
}

data class ConfCase(
    val name: String,
    val osm: List<String>,
    val geojson: List<String>,
    val netex: List<String>
) {
  override fun toString(): String {
    return "$name | ${osm.toStr()} | ${geojson.toStr()} | ${netex.toStr()}"
  }
}


fun List<String>.toStr() : String {
  return this.joinToString(" ")
}

fun String?.splt() : List<String> {
  return this?.split(regex = Regex("[ ,]+")) ?: listOf()
}

fun case(name : String, osm : String?, geojson: String?, netex : String?) : ConfCase {
  return ConfCase(name, osm = osm.splt(), geojson.splt(), netex = netex.splt())
}

fun expand(text : String, key : String, value: String, env : Map<String, String>) : String {
  var s = expand(text, key, value)
  for (e in env) {
    s = expand(s, e.key, e.value)
  }
  return s
}

fun expand(text : String, key : String, value: String) : String {
  return text.replace("{{$key}}", value)
}

