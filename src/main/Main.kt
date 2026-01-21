package org.entur.otp.setup

import org.entur.otp.setup.model.Config
import org.entur.otp.setup.model.Repository
import org.entur.otp.setup.model.SetupService
import org.entur.otp.setup.ui.SetupMainView
import org.entur.otp.setup.ui.UiModel
import java.io.File

fun main(args : Array<String>) {

  println("Args: ${args.joinToString(" ")}")
  val config = config()
  println(config)

  SetupMainView(config.cases) { run(it, config)}.setupAndStart()
}


fun run(input : UiModel, config: Config) {
  val repository = Repository(config)
  val case = repository.findSetupCase(input.selectedCase(), config.env)

  println(case)
  val targetDir = File(case.path)
  println("Setup ${targetDir.path}")

  if(input.includeNetex()) {
    SetupService.downloadNetexFiles(case.netex, targetDir)
  }
  if(input.includeOsm()) {
    SetupService.downloadAndFilterOsmFiles(case.osm, case.geojson, targetDir)
  }

  input.configDirectory()?.let {
    SetupService.copyConfigFiles(it, targetDir)
  }

  File(targetDir, "rt").mkdir()

  println("\nSETUP DONE!")
}
