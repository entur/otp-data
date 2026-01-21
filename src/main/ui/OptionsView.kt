package org.entur.otp.setup.ui

import org.entur.otp.setup.model.CONFIG_ROOT_DIR
import java.awt.Color
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JComboBox

private const val NO_CONFIG = "No config"

class OptionsView {
  private val panel: Box = Box.createVerticalBox()
  private val includeNetexChk: JCheckBox = JCheckBox(" 🚌 Netex (download and unpack)", true)
  private val includeOsmChk: JCheckBox = JCheckBox(" 🌍 OSM (download and link/filter)", false)
  private val configFilesCombo: JComboBox<String>

  fun includeNetex() : Boolean = includeNetexChk.isSelected
  fun includeOsm() : Boolean = includeOsmChk.isSelected
  fun configDirectory() : String? {
    val selected = configFilesCombo.selectedItem as? String
    return if (selected == NO_CONFIG) null else selected
  }

  init {
    panel.alignmentY = 0.5f
    panel.background = Color(0,0,0, 0)
    configFilesCombo = JComboBox(arrayOf(NO_CONFIG) + configDirectories())
    configFilesCombo.selectedItem = NO_CONFIG
    configFilesCombo.maximumSize = Dimension(160, 30)

    listOf(includeNetexChk, includeOsmChk, configFilesCombo).forEach { it.alignmentX = 0.0f }

    panel.add(includeNetexChk)
    panel.add(includeOsmChk)
    panel.add(configFilesCombo)

    includeNetexChk.toolTipText = """
      Download NeTEx files and unzip them into the nextex folder.
      Stops files (starting with "tiamat") is renamed to stat with "_stop".
    """.trimIndent()
    includeOsmChk.toolTipText = """
      Download OSM file if the file is more than 14 days old. Then filter
      the OSM file using the specified geojson.
    """.trimIndent()
    configFilesCombo.toolTipText = """
      Copy the config files from the selected directory into the setup case.
      Edit the files to adjust it to your test. For example adding real-time updaters.
    """.trimIndent()
  }

  fun panel() : Box = panel

  /** Get list of config directories */
  fun configDirectories() : Array<String> {
    if (CONFIG_ROOT_DIR.exists() && CONFIG_ROOT_DIR.isDirectory) {
      val configDirs = CONFIG_ROOT_DIR.listFiles()
          ?.filter { it.isDirectory }
          ?.map { it.name }
          ?.sorted()
          ?.toList()

      if(configDirs != null) {
        return configDirs.toTypedArray()
      }
    }
    return arrayOf()
  }
}