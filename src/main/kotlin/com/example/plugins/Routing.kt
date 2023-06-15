package com.example.plugins

import com.example.routing.authenticationRoutes
import com.example.routing.notesRoutes
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Home")
        }
    }
    notesRoutes()
    authenticationRoutes()
}
