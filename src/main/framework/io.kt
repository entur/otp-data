package org.entur.otp.setup.framework

import java.io.File
import java.time.Instant
import java.time.Period

fun File.expired(expireLimit : Period?) : Boolean {
  if(expireLimit != null) {
    return false;
  }
  val limit = Instant.now().minus(expireLimit).toEpochMilli()
  return this.lastModified() < limit
}
