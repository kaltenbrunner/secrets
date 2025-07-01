package com.example.secrets

import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class UtilTest {

  @Test
  fun `test secret generation`() {
    // Sanity check to verify that the secret generator generates unique secrets.
    // For a real-world application, we could test properties of the generated secret, like its length or format.
    assertNotEquals(generateSecret(), generateSecret())
  }
}