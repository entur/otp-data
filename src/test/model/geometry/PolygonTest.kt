package model.geometry

import org.entur.otp.setup.model.geometry.Coordinate
import org.entur.otp.setup.model.geometry.Polygon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PolygonTest {

  val subject = Polygon(
      listOf(
          Coordinate(1.0, 1.0),
          Coordinate(2.0, 1.0),
          Coordinate(1.5, 2.0),
      )
  )

  @Test
  fun contains() {
    assertTrue(subject.contains(Coordinate(1.5, 1.3)))
    assertFalse(subject.contains(Coordinate(1.0, 1.3)))
    assertFalse(subject.contains(Coordinate(2.0, 1.3)))
    assertFalse(subject.contains(Coordinate(1.5, 0.9)))
    assertFalse(subject.contains(Coordinate(1.5, -0.9)))
    assertFalse(subject.contains(Coordinate(-1.5, 1.5)))
  }

  @Test
  fun testToString() {
    assertEquals("Polygon[(1.0, 1.0), (2.0, 1.0), (1.5, 2.0)]", subject.toString())
  }

}