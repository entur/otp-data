package org.entur.otp.setup.ui

import org.entur.otp.setup.model.CONFIG_ROOT_DIR
import java.awt.Color
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel

private const val NO_CONFIG = "<Select %s>"

class OptionsView {
  private val panel: Box = Box.createVerticalBox()
  private val includeNetexChk: JCheckBox = JCheckBox(" 🚌 Netex (download and unpack)", true)
  private val includeOsmChk: JCheckBox = JCheckBox(" 🌍 OSM (download and link/filter)", false)
  private val defaultConfigCombo: JComboBox<String> = createConfigComboBox(NO_CONFIG.format("Default"), true)
  private val mainConfigCombo: JComboBox<String> = createConfigComboBox(NO_CONFIG.format("Main"), false)

  fun includeNetex() : Boolean = includeNetexChk.isSelected
  fun includeOsm() : Boolean = includeOsmChk.isSelected
  fun defaultConfig() : String? = selectedConfig(defaultConfigCombo)
  fun mainConfig() : String? = selectedConfig(mainConfigCombo)

  init {
    panel.alignmentY = 0.5f
    panel.background = Color(0,0,0, 0)


    includeNetexChk.toolTipText = """
      Download NeTEx files and unzip them into the nextex folder.
      Stops files (starting with "tiamat") is renamed to stat with "_stop".
    """.trimIndent()
    includeOsmChk.toolTipText = """
      Download OSM file if the file is more than 14 days old. Then filter
      the OSM file using the specified geojson.
    """.trimIndent()
    defaultConfigCombo.toolTipText = """
      Copy the config files from the selected directory into the setup case.
      Edit the files to adjust it to your test. For example adding real-time updaters.
      
      If you select both a 'default' and a 'main' configuration set, then the the default 
      is copied first and the main next - replacing exisiting files.
    """.trimIndent()
    mainConfigCombo.toolTipText = defaultConfigCombo.toolTipText

    val verticalSep = Box.createVerticalStrut(10) as JComponent
    val configLbl = JLabel("Copy config files. <Main> files will overwrite <Default> files, if selected.")

    var configBoxes = Box.createHorizontalBox();
    configBoxes.add(defaultConfigCombo)
    configBoxes.add(mainConfigCombo)
    configBoxes.add(Box.createHorizontalGlue())

    listOf(includeNetexChk, includeOsmChk, verticalSep, configLbl, configBoxes).forEach {
      it.alignmentX = 0.0f
      panel.add(it)
    }
  }

  fun panel() : Box = panel

  /** Get list of config directories */
  private fun configDirectories(defaultConfig : Boolean) : Array<String> {
    val defaultConfigs = listOf("dev", "staging", "prod")

    if (CONFIG_ROOT_DIR.exists() && CONFIG_ROOT_DIR.isDirectory) {
      val configDirs = CONFIG_ROOT_DIR.listFiles()
          ?.filter { it.isDirectory }
          ?.map { it.name }
          ?.sorted()
          ?.filter{ defaultConfig xor !defaultConfigs.contains(it) }
          ?.toList()

      if(configDirs != null) {
        return configDirs.toTypedArray()
      }
    }
    return arrayOf()
  }

  private fun selectedConfig(combo: JComboBox<String>) : String? {
    return if(combo.selectedIndex == 0) null else combo.selectedItem as? String
  }

  private fun createConfigComboBox(notSelectedText : String, defaultConfig : Boolean) : JComboBox<String> {
    val combo = JComboBox(arrayOf(notSelectedText) + configDirectories(defaultConfig))
    combo.selectedItem = NO_CONFIG
    combo.maximumSize = Dimension(160, 30)
    return combo
  }
}