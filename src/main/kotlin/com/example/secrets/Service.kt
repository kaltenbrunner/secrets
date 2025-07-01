package com.example.secrets

import com.example.secrets.model.Secret
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class Service {

  // Note: For a real-world application, we would need a way to remove quarantined secrets periodically. A scheduled
  // background task could clean up expired secrets on appropriate intervals, maybe every few minutes or every hour
  // depending on the amount of secrets being created. But I leave it out for now.

  private val secrets = ConcurrentHashMap<String, Secret>()

  fun createSecret(id: String, ttlSeconds: Int): String {
    val createdAt = Instant.now()
    val expiresAt = createdAt.plusSeconds(ttlSeconds.toLong())

    val secret = Secret(
      id,
      generateSecret(),
      createdAt,
      expiresAt,
      false
    )

    val existingSecret = secrets.putIfAbsent(id, secret)
    if (existingSecret != null) {
      throw SecretIdUnavailableException()
    }

    return secret.value
  }

  fun getSecret(id: String): String {
    val secret = secrets.compute(id) { _, secret ->
      if (secret == null) {
        throw SecretNotFoundException()
      }

      if (secret.hasExpired()) {
        throw SecretExpiredException()
      }

      if (secret.consumed) {
        throw SecretAlreadyConsumedException()
      }

      secret.copy(consumed = true)
    }

    return secret?.value ?: error("Secret is null after compute")
  }

  fun clearSecrets() {
    secrets.clear()
  }
}