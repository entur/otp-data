package org.entur.otp.setup.ui

interface UiModel {
  fun selectedCase() : String
  fun includeNetex() : Boolean
  fun includeOsm() : Boolean
  fun includeConfigFiles() : Boolean
}