package org.entur.otp.setup.ui

import javax.swing.Box
import javax.swing.JCheckBox

class OptionsView {
  val panel: Box = Box.createVerticalBox()
  private val includeNetexChk: JCheckBox = JCheckBox(" 🚌 Netex (download and unpack)", true)
  private val includeOsmChk: JCheckBox = JCheckBox(" 🌍 OSM (download and link/filter)", false)
  private val includeConfigFilesChk: JCheckBox = JCheckBox(" 🛠 Config files (copy)", false)

  fun includeNetex() : Boolean = includeNetexChk.isSelected
  fun includeOsm() : Boolean = includeOsmChk.isSelected
  fun includeConfigFiles() : Boolean = includeConfigFilesChk.isSelected

  constructor() {
    panel.add(Box.createGlue())
    panel.add(includeNetexChk)
    panel.add(Box.createGlue())
    panel.add(includeOsmChk)
    panel.add(Box.createGlue())
    panel.add(includeConfigFilesChk)
    panel.add(Box.createGlue())

    includeNetexChk.toolTipText = """
      Download NeTEx files and unzip them into the nextex folder. 
      Stops files (starting with "tiamat") is renamed to stat with "_stop".
    """.trimIndent()
    includeOsmChk.toolTipText = """
      Download OSM file if the file is more than 14 days old. Then filter
      the OSM file using the specified geojson.  
    """.trimIndent()
    includeConfigFilesChk.toolTipText = """
      Copy the config files into the setup case. Edit the files to adjust
      it to your test. For example adding real-time updaters.  
    """.trimIndent()
  }
}