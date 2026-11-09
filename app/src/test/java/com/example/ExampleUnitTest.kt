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

  @Test
  fun testCardBrandDetection() {
    val visaBrand = com.example.data.PaymentGatewayManager.detectCardBrand("4111111111111111")
    assertEquals(com.example.data.CardBrand.VISA, visaBrand)

    val mcBrand = com.example.data.PaymentGatewayManager.detectCardBrand("5100000000000000")
    assertEquals(com.example.data.CardBrand.MASTERCARD, mcBrand)

    val jcbBrand = com.example.data.PaymentGatewayManager.detectCardBrand("3528000000000000")
    assertEquals(com.example.data.CardBrand.JCB, jcbBrand)
  }

  @Test
  fun testDigitalWalletLinking() {
    val result = com.example.data.PaymentGatewayManager.linkDigitalWallet(
      walletType = "MOMO",
      phoneNumber = "0987654321",
      accountName = "Nguyen Van A"
    )
    assertTrue(result.isSuccess)
    val method = result.getOrNull()
    assertNotNull(method)
    assertEquals("Ví MoMo", method?.title)
    assertEquals("0987654321", method?.lastFour)
  }
}
