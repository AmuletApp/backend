package com.github.redditvanced.utils

import io.github.cdimascio.dotenv.dotenv
import java.io.File
import kotlin.system.exitProcess

object DotEnv {
	fun loadDotEnv() {
		val env = File(".env.local")
		if (!env.exists()) {
			val classLoader = GithubUtils::class.java.classLoader
			val defaultEnvUri = classLoader.getResource(".env.default")
				?: error("Failed to load default config from jar")
			env.writeText(defaultEnvUri.readText())

			println("env config missing, generating and exiting. Please fill out the configuration and retry.")
			exitProcess(1)
		}

		dotenv {
			filename = ".env.local"
			systemProperties = true
		}
	}
}
