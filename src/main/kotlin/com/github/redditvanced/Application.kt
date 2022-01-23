package com.github.redditvanced

import com.github.redditvanced.plugins.configureMonitoring
import com.github.redditvanced.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.exposed.sql.Database

fun main() {
    Database.connect("jdbc:sqlite:./data.db")

    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureMonitoring()
    }
    server.start(wait = true)
}
