package org.entur.otp.setup.csv

import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.forEachLine

fun main() {
  val inFile = "/Users/thomasgran/code/entur/otp/data/otp/fylker/rogaland/empirical_delay/typical_delay_stop_level.txt"
  val outFile = "/Users/thomasgran/code/entur/otp/data/otp/fylker/rogaland/empirical_delay/empirical_delay_trip_times.txt"

  val out = Path(outFile).bufferedWriter()
  val inPath = Path(inFile)
  var lineCounter = 0
  var includedLinesCounter = 0

  inPath.forEachLine { line ->
    ++lineCounter
    if(lineCounter % 100 == 0) {
      print(".")
      if(lineCounter % 10_000 == 0) {
        println()
      }
    }
    if(line.contains("KOL:") || lineCounter == 1) {
      ++includedLinesCounter
      out.write("$line, 216")
      out.newLine()
    }
  }
  out.flush()
  out.close()

  println()
  println("%,d".format(includedLinesCounter) + " included of total " + "%,d".format(lineCounter) + " lines.")
}