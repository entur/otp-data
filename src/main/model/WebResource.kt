package org.entur.otp.setup.model

import java.io.File
import java.time.Period
import org.entur.otp.setup.framework.expired
import org.entur.otp.setup.framework.download
import org.entur.otp.setup.framework.mkDirs
import org.entur.otp.setup.framework.unzip

data class WebResource(val name : String, val url: String, val webFilename: String) {

  val localFilename = webFilename.replace("%20", "-")

  fun asFile(dir : File) = File(dir, localFilename)

  override fun toString(): String {
    return "$localFilename $url/$webFilename"
  }

  fun downloadNetexFile(targetDir : File, netexTargetDir : String, expire : Period? = null) {
    downloadFile(targetDir, expire)
    unzip(localFilename, netexTargetDir, targetDir)
    asFile(targetDir).delete()
  }

  fun downloadFile(targetDir : File, expire : Period? = null) {
    mkDirs(targetDir)
    val target = asFile(targetDir)
    if(target.exists()) {
      if(target.expired(expire)) {
        target.delete();
      }
      else {
        println("Skip download, file is newer then ${expire?.days} days ($webFilename).")
        return
      }
    }
    println("Download '$webFilename' ($url)")
    download("$url/$webFilename", localFilename, targetDir)
  }
}