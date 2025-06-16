package org.entur.otp.setup.ui

import org.entur.otp.setup.model.SetupCaseConfig
import org.entur.otp.setup.ui.UiConstants.DEFAULT_INSETS
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.function.Consumer
import javax.swing.*

class SetupMainView(
    cases : List<SetupCaseConfig>,
    val runSetup: Consumer<UiModel>
) : UiModel {
  var y = 0

  private val mainFrame = JFrame("Setup OTP")
  private val casesView = SetupCasesView(cases).init()
  private val options = OptionsView()

  fun setupAndStart() {
    val innerPanel = JPanel()

    val layout = GridBagLayout()
    innerPanel.setLayout(layout)
    innerPanel.setBackground(UiConstants.BACKGROUND)
    innerPanel.add(casesView.panel(), gbc())
    innerPanel.add(options.panel, gbc(weightx = 0.0, weighty = 0.0, fill = GridBagConstraints.NONE))
    innerPanel.add(runButton(), gbc(fill = GridBagConstraints.NONE))

    mainFrame.setContentPane(JScrollPane(innerPanel))
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    mainFrame.pack()
    mainFrame.setLocationRelativeTo(null)
    mainFrame.setVisible(true)
  }

  override fun selectedCase() = casesView.selectedCase
  override fun includeNetex() = options.includeNetex()
  override fun includeOsm() = options.includeOsm()
  override fun includeConfigFiles() = options.includeConfigFiles()


  private fun runButton() : JButton {
    val btn = JButton(" ❯ Run ")
    btn.addActionListener {
      runSetup.accept(this)
      mainFrame.isVisible = false
      mainFrame.dispose()
    }
    return btn
  }

  private fun gbc(
      weightx: Double = 1.0,
      weighty: Double = 1.0,
      insets: Insets = DEFAULT_INSETS,
      fill: Int = GridBagConstraints.HORIZONTAL,
      ipadx : Int = 0
  ): GridBagConstraints {
    return GridBagConstraints(
        0,
        y++,
        1,
        1,
        weightx,
        weighty,
        GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL,
        insets,
        ipadx,
        0
    )
  }
}