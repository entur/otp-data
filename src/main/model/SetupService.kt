package org.entur.otp.setup.model

import org.entur.otp.setup.framework.execEcho
import org.entur.otp.setup.framework.link
import org.entur.otp.setup.framework.renameFiles
import org.entur.otp.setup.framework.rmDir
import java.io.File
import java.time.Period


object SetupService {

  fun copyConfigFiles(configDirName: String, targetDir: File) {
    println("\nCopy config files from '$configDirName' to ${targetDir.path}")
    val configDir = File(CONFIG_ROOT_DIR, configDirName)
    if (!configDir.exists() || !configDir.isDirectory) {
      println("  Warning: Config directory '${configDir.path}' does not exist")
      return
    }
    val configFiles = configDir.listFiles { it.name.endsWith("-config.json") }
    configFiles?.forEach { file ->
      println("  - ${file.name}")
      val target = File(targetDir, file.name)
      val config = file.readText(Charsets.UTF_8).replace("{{OTP_SETUP_PATH}}", targetDir.absolutePath)
      target.writeText(config, Charsets.UTF_8)
    }
  }

  fun downloadAndFilterOsmFiles(osmResources: List<WebResource>, geojson : List<FileResource>, targetDir: File) {
    val expire = Period.ofDays(14)
    val osmTarget = File("osm")

    osmResources.forEach { it.downloadFile(osmTarget, expire) }

    if(geojson.isEmpty()) {
      println("Create link in target for each osm file")
      osmResources.forEach { link(it.file, osmTarget, targetDir) }
    }
    else if(osmResources.size == 1) {
      val osm = osmResources.first()
      println("Filter the osm file ${osm.file} using geojson: $geojson")
      geojson.forEach { filterOsm(osm, it, osmTarget, targetDir) }
    }
    else if(geojson.size == 1) {
      val filter = geojson.first()
      println("Filter each osm file using the $filter geojson filter.")
      osmResources.forEach { filterOsm(it, geojson.first(), osmTarget, targetDir) }
    }
    else if(geojson.size == osmResources.size) {
      println("Filter each osm file using the corresponding geojson filter.")
      for (i in 0..geojson.size-1) {
        filterOsm(osmResources[i], geojson[i], osmTarget, targetDir)
      }
    }
    else {
      throw Exception(
          "Inconsistent data. Expected zero or one geojson file OR a geojson file for each " +
              "osm file. Number of OSM/GEOJSON files: ${osmResources.size}/${geojson.size}."
      )
    }
  }

  fun filterOsm(osm: WebResource, geojson: FileResource, osmTarget: File, targetDir: File) {
    val target = File(targetDir,"${osm.name}_${geojson}.osm.pbf")
    val source = osm.asFile(osmTarget)
    println("Filter ${source.name} using $geojson polygon...")

    if(target.exists()) {
      if(target.lastModified() < source.lastModified()) {
        target.delete();
      }
      else {
        println("Skip filtering OSM file, target is newer. ")
        return
      }
    }
    val geojsonFile = geojson.asFile().absolutePath
    val tempFile = File(targetDir, "temp.osm.pbf")

    "osmium extract --overwrite -v --polygon \"$geojsonFile\" \"${source.absolutePath}\" -o ${tempFile.name}".execEcho(targetDir)
    "osmium tags-filter ${tempFile.name} w/highway w/public_transport=platform w/railway=platform w/park_ride r/type=restriction --overwrite -o \"${target.name}\"".execEcho(targetDir)
    tempFile.delete()
  }

  fun downloadNetexFiles(cases: List<WebResource>, targetDir: File) {
    val expire = Period.ofDays(1)
    val netexTargetDir = File(targetDir, "netex")
    rmDir(netexTargetDir.name, targetDir)

    for (it in cases) {
      println("Download ${it.url}/${it.file} ($targetDir)")
      it.downloadNetexFile(targetDir, netexTargetDir.name, expire)
    }

    // Rename stop files to follow the OTP naming convention
    renameFiles("tiamat", "_stops_tiamat", netexTargetDir)
  }
}