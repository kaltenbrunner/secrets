package com.example.secrets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SecretsApplication

fun main(args: Array<String>) {
	runApplication<SecretsApplication>(*args)
}
