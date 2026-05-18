package org.entur.otp.setup

import org.entur.otp.setup.model.Config
import org.entur.otp.setup.model.Repository
import org.entur.otp.setup.model.SetupService
import org.entur.otp.setup.ui.CliModel
import org.entur.otp.setup.ui.SetupMainView
import org.entur.otp.setup.ui.UiModel
import java.io.File

fun main(args : Array<String>) {
  val config = config()

  if (args.isEmpty()) {
    SetupMainView(config.cases) { run(it, config) }.setupAndStart()
  } else if (args.contains("--help")) {
    CliModel.printHelp(config.cases.map { it.path })
  } else {
    run(CliModel.parse(args, config.cases.map { it.path }), config)
  }
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
  if(input.filterNetex()) {
    SetupService.filterNetex(case.geojson, targetDir)
  }
  if(input.includeOsm()) {
    SetupService.downloadAndFilterOsmFiles(case.osm, case.geojson, targetDir)
  }

  input.defaultConfig()?.let {
    SetupService.copyConfigFiles(it, targetDir)
  }
  input.mainConfig()?.let {
    SetupService.copyConfigFiles(it, targetDir)
  }

  File(targetDir, "rt").mkdir()

  println("\nSETUP DONE!")
}
