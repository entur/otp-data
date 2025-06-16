package org.entur.otp.setup.framework

fun List<String>.toStr() : String {
  return this.joinToString(" ")
}

fun String?.splt() : List<String> {
  return this?.split(regex = Regex("[ ,]+"))?.filter { !it.isBlank() } ?: listOf()
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
