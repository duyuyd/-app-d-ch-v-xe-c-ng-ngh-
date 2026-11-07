package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testUserLocationCreation() {
    val loc = com.example.data.UserLocation(
      latitude = 21.0285,
      longitude = 105.8542,
      address = "Hà Nội, Việt Nam",
      accuracy = 5.0f
    )
    assertEquals(21.0285, loc.latitude, 0.0001)
    assertEquals(105.8542, loc.longitude, 0.0001)
    assertEquals("Hà Nội, Việt Nam", loc.address)
  }
}
