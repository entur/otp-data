package org.entur.otp.setup.ui

import org.entur.otp.setup.model.SetupCaseConfig
import org.entur.otp.setup.ui.UiConstants.SECTION_SPACE
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*


class SetupCasesView(private val cases: List<SetupCaseConfig>) {
  /*
   |-----------------------------------------------|
   | Label                                         |
   |-----------------------------------------------|
   |  ( ) List 1   |  ( ) List 2   |  ( ) List 3   |
   |  ( ) List 1   |  ( ) List 2   |  ( ) List 3   |
   |-----------------------------------------------|
  */
  private val mainPanel: Box = Box.createVerticalBox()
  private val listPanel: Box = Box.createHorizontalBox()
  var selectedCase: String = cases[0].path

  fun init() : SetupCasesView {
    setupDataSources()
    mainPanel.add(JLabel("Select data source:"))
    mainPanel.add(Box.createVerticalStrut(SECTION_SPACE))
    mainPanel.add(Box.createVerticalStrut(SECTION_SPACE))
    listPanel.setAlignmentX(Component.LEFT_ALIGNMENT)
    mainPanel.add(listPanel)
    listPanel.repaint()
    return this
  }

  fun panel(): JComponent {
    return mainPanel
  }

  private fun setupDataSources() {
    if (cases.isEmpty()) {
      val label = JLabel("<No otp configuration files found>")
      listPanel.add(label)
      return
    }

    val selectDataSourceRadioGroup = ButtonGroup()

    val casesSorted = cases.sorted()
    val size = casesSorted.size

    // Split the list of configuration in one, two or three columns depending on the
    // number of configurations found.
    if (size <= 10) {
      addListPanel(casesSorted, selectDataSourceRadioGroup)
    } else if (size <= 20) {
      val half = size / 2
      addListPanel(casesSorted.subList(0, half), selectDataSourceRadioGroup)
      addHorizontalGlue(listPanel)
      addListPanel(casesSorted.subList(half, size), selectDataSourceRadioGroup)
    } else {
      val third = size / 3
      addListPanel(casesSorted.subList(0, third), selectDataSourceRadioGroup)
      addHorizontalGlue(listPanel)
      addListPanel(
          casesSorted.subList(third, third * 2),
          selectDataSourceRadioGroup
      )
      addHorizontalGlue(listPanel)
      addListPanel(
          casesSorted.subList(third * 2, size),
          selectDataSourceRadioGroup
      )
    }
  }

  private fun addListPanel(
      values: List<SetupCaseConfig>,
      selectDataSourceRadioGroup: ButtonGroup
  ) {
    val column = Box.createVerticalBox()

    for (case in values) {
      val selected = selectedCase == case.path
      val radioBtn: JRadioButton = newRadioBtn(selectDataSourceRadioGroup, case, selected)
      radioBtn.addActionListener(ActionListener { e: ActionEvent? -> this.onCaseChange(e) })
      column.add(radioBtn)
    }
    listPanel.add(column)
  }

  fun onCaseChange(e: ActionEvent?) {
    selectedCase = e?.actionCommand ?: selectedCase
  }

  companion object {
    private fun newRadioBtn(group: ButtonGroup, case: SetupCaseConfig, selected: Boolean): JRadioButton {
      val radioButton = JRadioButton(case.path, selected)
      radioButton.toolTipText = case.description()
      group.add(radioButton)
      return radioButton
    }

    private fun addHorizontalGlue(box: Box) {
      box.add(Box.createHorizontalGlue())
    }
  }
}
