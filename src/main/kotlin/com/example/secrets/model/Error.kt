package com.example.secrets.model

sealed interface ErrorResponse

data class GeneralErrorResponse(
  val message: String,
  val reason: ErrorReason
) : ErrorResponse

// The OpenAPI specification didn't mention a specific error response for validation errors, but it seems reasonable to
// include one. Thus, INVALID_INPUT is also added as an error reason below.
data class ValidationErrorResponse(
  val message: String,
  val reason: ErrorReason,
  val fieldErrors: Map<String, String>
) : ErrorResponse

enum class ErrorReason {
  INVALID_INPUT,
  ID_UNAVAILABLE,
  NOT_FOUND,
  ALREADY_CONSUMED,
  EXPIRED
}