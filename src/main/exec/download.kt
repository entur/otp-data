package exec

import config.model.WebResource
import java.io.File
import java.time.Instant
import java.time.Period

fun downloadNetexFile(r : WebResource, targetDir : File, netexTarget : File, expire : Period? = null) {
  downloadFile(r, targetDir, expire)
  "unzip -o ${r.file} -d ${netexTarget.name}".exec(targetDir)
  r.asFile(targetDir).delete()
}

fun downloadFile(r : WebResource, targetDir : File, expire : Period? = null) {
  mkDirs(targetDir)
  val target = r.asFile(targetDir)
  if(target.exists()) {
    if(expired(target, expire)) {
      target.delete();
    }
    else {
      println("Skip download, file is newer then ${expire?.days} days (${r.file}).")
      return
    }
  }
  println("Download '${r.file}' (${r.url})")
  "wget --progress=dot:giga ${r.url}/${r.file}".exec(targetDir)
}
