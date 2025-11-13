package org.entur.otp.setup.model

import java.io.File

data class FileResource(val name : String, val extension : String, val path : File) {

  fun asFile() : File = File(path, "$name.$extension")

  override fun toString() : String = "${name}.${extension}"

}
