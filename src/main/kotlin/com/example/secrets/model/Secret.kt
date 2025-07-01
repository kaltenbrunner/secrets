package com.example.secrets.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateSecretRequest(
  @field:NotBlank(message = "ID is required.")
  val id: String,

  @field:Min(value = 1, message = "TTL must be at least 1 second.")
  val ttlSeconds: Int
)

data class SecretResponse(
  val secret: String
)

data class Secret(
  val id: String,
  val value: String,
  val createdAt: Instant,
  val expiresAt: Instant,
  val consumed: Boolean,
) {
  fun hasExpired(): Boolean = Instant.now().isAfter(expiresAt)
}