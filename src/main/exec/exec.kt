package exec

import java.io.File
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.time.Instant
import java.time.Period
import java.util.concurrent.TimeUnit

fun String.execEcho(workingDir: File) = exec(workingDir, this, echo=true)
fun String.exec(workingDir: File) = exec(workingDir, this)

fun exec(workingDir: File, cmdLine : String, echo : Boolean = false) {
  val args = cmdLine.splitCmdLine()
  if(echo) {
    println("  > " + args.joinToString(" "))
  }
  val p = ProcessBuilder(args)
      .directory(workingDir)
      .redirectOutput(INHERIT)
      .redirectError(INHERIT)
      .start()
  if(!p.waitFor(10, TimeUnit.MINUTES)) {
    throw Exception("Process timeout!")
  }
  if(p.exitValue() != 0) {
    throw Exception("Process failed! Exit value=${p.exitValue()}")
  }
}

fun rmDir(dirname : String, targetDir : File) {
  if(!targetDir.exists()) {
    println("Target dir does not exist: $targetDir")
    return
  }
  "rm -Rf $dirname".execEcho(targetDir)
}

fun mkDirs(targetDir : File) {
  if(!targetDir.exists()) {
    if(!targetDir.mkdirs()) {
      throw IllegalStateException("ERROR! Unable to create target dir: $targetDir")
    }
  }
}

fun renameFiles(oldValue : String, newValue : String, targetDir: File) {
  for (file in targetDir.listFiles()) {
    if(file.nameWithoutExtension.contains(oldValue, ignoreCase = true)) {
      val newName = file.name.replace(oldValue, newValue)
      println("Rename ${file.name} to $newName in $targetDir")
      file.renameTo(File(targetDir, newName))
    }
  }
}

fun link(filename : String, srcDir : File, targetDir : File) {
  "ln -fsv ${srcDir.absolutePath}/${filename} ${filename}".execEcho(targetDir)
}

fun expired(file : File, expireLimit : Period?) : Boolean {
  if(expireLimit != null) {
    return false;
  }
  val limit = Instant.now().minus(expireLimit).toEpochMilli()
  return file.lastModified() < limit
}


/**
 *
 */
fun String.splitCmdLine() : List<String> {
  val list = arrayListOf<String>()
  val quote = arrayListOf<String>()

  val tokens = this.split(' ')
  var state = 0
  var quoteCh : Char = ' '

  for (s in tokens) {
    when(state) {
      0 -> {
        if("\"'".contains(s.first())) {
          if(s.first() == s.last()) {
            list.add(s.substring(1, s.length-1))
          }
          else {
            quote.add(s)
            state = 1
          }
        }
        else {
          list.add(s)
        }
      }
      1 -> {
        if(quoteCh == s.last()) {
          quote.add(s.substring(0, s.length-1))
          list.add(quote.joinToString(" "))
          quote.clear()
          state = 0
        }
        else {
          quote.add(s)
        }
      }
    }
  }
  return list
}

