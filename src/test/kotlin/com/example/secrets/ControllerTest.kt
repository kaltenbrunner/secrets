package com.example.secrets

import com.example.secrets.model.CreateSecretRequest
import com.example.secrets.model.ErrorReason
import com.example.secrets.model.GeneralErrorResponse
import com.example.secrets.model.SecretResponse
import com.example.secrets.model.ValidationErrorResponse
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.lang.Thread.sleep
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ControllerTest {

  @LocalServerPort
  private var port: Int = 0

  @Autowired
  lateinit var restTemplate: TestRestTemplate

  @Autowired
  lateinit var service: Service

  @BeforeEach
  fun setup() {
    service.clearSecrets()
  }

  @Test
  fun `create and retrieve secret`() {
    val id = randomId()

    val request = CreateSecretRequest(id, 10)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)

    assertEquals(HttpStatus.CREATED, createResponse.statusCode)
    val createdSecret = createResponse.body!!.secret
    assertNotNull(createdSecret)
    assertTrue(createdSecret.isNotBlank())

    val getResponse = restTemplate.getForEntity(url("/secrets/$id"), SecretResponse::class.java)

    assertEquals(HttpStatus.OK, getResponse.statusCode)
    assertEquals(createdSecret, getResponse.body!!.secret)
  }

  @Test
  fun `validate null secret id`() {
    val json = """{"id": null, "ttlSeconds": 10}"""
    val headers = HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }
    val entity = HttpEntity(json, headers)
    val errorResponse = restTemplate.postForEntity(url("/secrets"), entity, GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.INVALID_INPUT, errorResponse.body!!.reason)
  }

  @Test
  fun `validate empty secret id`() {
    val request = CreateSecretRequest("", 10)
    val errorResponse = restTemplate.postForEntity(url("/secrets"), request, ValidationErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.INVALID_INPUT, errorResponse.body!!.reason)
    assertNotNull(errorResponse.body!!.fieldErrors)
  }

  @ParameterizedTest
  @ValueSource(ints = [0, -1])
  fun `validate positive ttlSeconds`(ttlSeconds: Int) {
    val request = CreateSecretRequest(randomId(), ttlSeconds)
    val errorResponse = restTemplate.postForEntity(url("/secrets"), request, ValidationErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.INVALID_INPUT, errorResponse.body!!.reason)
    assertNotNull(errorResponse.body!!.fieldErrors)
  }

  @Test
  fun `secret id is unavailable if already used`() {
    val request = CreateSecretRequest(randomId(), 10)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    val errorResponse = restTemplate.postForEntity(url("/secrets"), request, GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.ID_UNAVAILABLE, errorResponse.body!!.reason)
  }

  @Test
  fun `secret id is unavailable if secret is consumed`() {
    val id = randomId()
    val request = CreateSecretRequest(id, 10)

    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    val getResponse = restTemplate.getForEntity(url("/secrets/$id"), SecretResponse::class.java)
    assertEquals(HttpStatus.OK, getResponse.statusCode)

    val errorResponse = restTemplate.postForEntity(url("/secrets"), request, GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.ID_UNAVAILABLE, errorResponse.body!!.reason)
  }

  @Test
  fun `secret id is unavailable if secret is expired`() {
    val request = CreateSecretRequest(randomId(), 1)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    // Wait for the secret to expire
    // Note: In a real-world application, we would use a more reliable way to wait for expiration, such as mocking time.
    sleep(1500)

    val errorResponse = restTemplate.postForEntity(url("/secrets"), request, GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.BAD_REQUEST, errorResponse.statusCode)
    assertEquals(ErrorReason.ID_UNAVAILABLE, errorResponse.body!!.reason)
  }

  @Test
  fun `secret not found`() {
    val errorResponse = restTemplate.getForEntity(url("/secrets/unknown"), GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.NOT_FOUND, errorResponse.statusCode)
    assertEquals(ErrorReason.NOT_FOUND, errorResponse.body!!.reason)
  }

  @Test
  fun `secret has expired`() {
    val id = randomId()

    val request = CreateSecretRequest(id, 1)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    // Wait for the secret to expire
    sleep(1500)

    val errorResponse = restTemplate.getForEntity(url("/secrets/$id"), GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.NOT_FOUND, errorResponse.statusCode)
    assertEquals(ErrorReason.EXPIRED, errorResponse.body!!.reason)
  }

  @Test
  fun `secret already consumed`() {
    val id = randomId()

    val request = CreateSecretRequest(id, 10)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    val getResponse = restTemplate.getForEntity(url("/secrets/$id"), SecretResponse::class.java)
    assertEquals(HttpStatus.OK, getResponse.statusCode)

    val errorResponse = restTemplate.getForEntity(url("/secrets/$id"), GeneralErrorResponse::class.java)
    assertEquals(HttpStatus.NOT_FOUND, errorResponse.statusCode)
    assertEquals(ErrorReason.ALREADY_CONSUMED, errorResponse.body!!.reason)
  }

  @Test
  fun `concurrent access to the same secret`() {
    val id = randomId()
    val request = CreateSecretRequest(id, 10)
    val createResponse = restTemplate.postForEntity(url("/secrets"), request, SecretResponse::class.java)
    assertEquals(HttpStatus.CREATED, createResponse.statusCode)

    val threadCount = 5
    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)
    val threads = List(threadCount) {
      Thread {
        try {
          latch.await()
          val getResponse = restTemplate.getForEntity(url("/secrets/$id"), SecretResponse::class.java)
          if (getResponse.statusCode == HttpStatus.OK) {
            successCount.incrementAndGet()
          }
        } catch (e: Exception) {
          // Threads that fail to get the secret will throw exceptions
        }
      }.apply { start() }
    }

    latch.countDown()
    threads.forEach { it.join() }

    assertEquals(1, successCount.get())
  }

  private fun url(path: String = "") = "http://localhost:$port$path"

  private fun randomId() = UUID.randomUUID().toString()
}
