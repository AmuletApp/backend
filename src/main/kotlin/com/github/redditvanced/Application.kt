package com.github.redditvanced

import com.github.redditvanced.plugins.configureMonitoring
import com.github.redditvanced.plugins.configureRouting
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
//    Database.connect("jdbc:sqlite:./data.db")
//    runMigrations(listOf(M001()))

    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureMonitoring()
    }
    server.start(wait = true)
}
