package com.example.secrets

import com.example.secrets.model.ErrorReason
import com.example.secrets.model.ErrorResponse
import com.example.secrets.model.GeneralErrorResponse
import com.example.secrets.model.ValidationErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler {

  // In a real-world application, we would have more robust error handling, including a fallback handler
  // for unexpected errors. It could also be useful to have a library with shared HTTP exceptions to
  // standardize error handling across microservices.

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val fieldErrors = ex.bindingResult.fieldErrors.associate {
      it.field to (it.defaultMessage ?: "Invalid value")
    }
    val body = ValidationErrorResponse(
      "Invalid input provided.",
      ErrorReason.INVALID_INPUT,
      fieldErrors
    )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadableException(): ResponseEntity<ErrorResponse> {
    val body = GeneralErrorResponse(
      "Invalid JSON format or missing required fields.",
      ErrorReason.INVALID_INPUT
    )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
  }

  @ExceptionHandler(SecretException::class)
  fun handleSecretException(ex: SecretException): ResponseEntity<ErrorResponse> {
    val (status, body) = ex.toHttpErrorResponse()
    return ResponseEntity.status(status).body(body)
  }

  private fun SecretException.toHttpErrorResponse(): Pair<HttpStatus, ErrorResponse> = when (this) {
    is SecretIdUnavailableException -> HttpStatus.BAD_REQUEST to GeneralErrorResponse(
      "The provided ID is currently unavailable (in use or recently used).",
      ErrorReason.ID_UNAVAILABLE
    )

    is SecretNotFoundException -> HttpStatus.NOT_FOUND to GeneralErrorResponse(
      "No secret found with the provided ID.",
      ErrorReason.NOT_FOUND
    )

    is SecretAlreadyConsumedException -> HttpStatus.NOT_FOUND to GeneralErrorResponse(
      "This secret has already been retrieved.",
      ErrorReason.ALREADY_CONSUMED
    )

    is SecretExpiredException -> HttpStatus.NOT_FOUND to GeneralErrorResponse(
      "The secret is no longer available.",
      ErrorReason.EXPIRED
    )
  }
}