package config.model

data class WebResource(val name : String, val url: String, val file: String) {
  fun fullUrl() : String = "$url/$file"

  fun asFile(dir : File) = File(dir, file)
  override fun toString(): String {
    return "$name $url/$file"
  }
}