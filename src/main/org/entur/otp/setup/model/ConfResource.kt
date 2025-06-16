package org.entur.otp.setup.model

import org.entur.otp.setup.framework.toStr
import org.entur.otp.setup.framework.expand

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