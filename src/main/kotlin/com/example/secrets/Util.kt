package com.example.secrets

import java.util.UUID

fun generateSecret(): String = UUID.randomUUID().toString().replace("-", "")