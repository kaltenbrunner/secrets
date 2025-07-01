package com.example.secrets

import com.example.secrets.model.CreateSecretRequest
import com.example.secrets.model.SecretResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/secrets")
class Controller(
  private val service: Service
) {

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  fun createSecret(@Valid @RequestBody request: CreateSecretRequest): SecretResponse {
    val secret = service.createSecret(request.id, request.ttlSeconds)
    return SecretResponse(secret)
  }
  
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  fun getSecret(@PathVariable id: String): SecretResponse {
    val secret = service.getSecret(id)
    return SecretResponse(secret)
  }
}