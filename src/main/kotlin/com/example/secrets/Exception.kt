package com.example.secrets

sealed class SecretException() : RuntimeException()

class SecretIdUnavailableException() : SecretException()

class SecretNotFoundException() : SecretException()

class SecretAlreadyConsumedException() : SecretException()

class SecretExpiredException() : SecretException()
