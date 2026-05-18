package org.entur.otp.setup.ui

class CliModel private constructor(
    private val caseName: String,
    private val netex: Boolean,
    private val osm: Boolean,
    private val filterNetexEnabled: Boolean,
    private val defaultCfg: String?,
    private val mainCfg: String?
) : UiModel {

  override fun selectedCase() = caseName
  override fun includeNetex() = netex
  override fun filterNetex() = filterNetexEnabled
  override fun includeOsm() = osm
  override fun defaultConfig() = defaultCfg
  override fun mainConfig() = mainCfg

  companion object {
    fun printHelp(availableCases: List<String>) {
      println("""
        Usage: otp-data-setup <case> [options]

        Arguments:
          <case>                  Setup case to run. Can be the full path or just the last segment.

        Options:
          --netex                 Download NeTEx files (default if neither --netex nor --osm is given)
          --osm                   Download and filter OSM files
          --filter-netex          Filter NeTEx files using a GeoJSON polygon
          --config <path>         Copy default config files from the given directory
          --main-config <path>    Copy main config files from the given directory
          --help                  Show this help message

        Available cases:
      """.trimIndent())
      availableCases.forEach { println("  $it") }
    }

    fun parse(args: Array<String>, availableCases: List<String>): CliModel {
      val input = args[0]
      val caseName = availableCases.find { it == input }
          ?: availableCases.find { it.endsWith("/$input") }
          ?: throw IllegalArgumentException(
              "Case not found: '$input'\nAvailable cases:\n${availableCases.joinToString("\n") { "  $it" }}"
          )

      var netex = false
      var osm = false
      var filterNetex = false
      var defaultConfig: String? = null
      var mainConfig: String? = null

      var i = 1
      while (i < args.size) {
        when (args[i]) {
          "--netex" -> netex = true
          "--osm" -> osm = true
          "--filter-netex" -> filterNetex = true
          "--config" -> { i++; if (i < args.size) defaultConfig = args[i] }
          "--main-config" -> { i++; if (i < args.size) mainConfig = args[i] }
        }
        i++
      }

      // Default: download NeTEx if no data flags provided
      if (!netex && !osm && !filterNetex) netex = true

      return CliModel(caseName, netex, osm, filterNetex, defaultConfig, mainConfig)
    }
  }
}
