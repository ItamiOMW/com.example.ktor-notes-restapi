package com.example.routing

import com.example.authentication.TokenManager
import com.example.db.DatabaseConnection
import com.example.entities.UserEntity
import com.example.model.Response
import com.example.model.User
import com.example.model.UserCredentials
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.ktorm.dsl.*
import org.mindrot.jbcrypt.BCrypt


fun Application.authenticationRoutes() {
    val db = DatabaseConnection.database
    val tokenManager = TokenManager(HoconApplicationConfig(ConfigFactory.load()))

    routing {
        post("/register") {
            try {
                val userCredentials = call.receive<UserCredentials>()

                if (!userCredentials.isCredentialsValid()) {
                    val response = Response(
                        data = "Invalid credentials",
                        successful = false
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                val username = userCredentials.username.toLowerCasePreservingASCIIRules()
                val password = userCredentials.hashedPassword()

                val user = db.from(UserEntity)
                    .select()
                    .where { UserEntity.username eq username }
                    .map { it[UserEntity.username] }
                    .firstOrNull()

                if (user != null) {
                    val response = Response(
                        data = "User with this username already exists",
                        successful = false
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                val rowsEffected = db.insert(UserEntity) { userEntity ->
                    set(userEntity.username, username)
                    set(userEntity.password, password)
                }

                if (rowsEffected == 1) {
                    val response = Response("User created successfully", true)
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    val response = Response("Failed to create user", false)
                    call.respond(HttpStatusCode.BadRequest, response)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to create user", false)
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        post("/login") {
            try {
                val userCredentials = call.receive<UserCredentials>()

                if (!userCredentials.isCredentialsValid()) {
                    val response = Response(
                        data = "Invalid credentials",
                        successful = false
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                val username = userCredentials.username.toLowerCasePreservingASCIIRules()
                val password = userCredentials.password

                val user = db.from(UserEntity)
                    .select()
                    .where { UserEntity.username eq username }
                    .map {
                        val id = it[UserEntity.id] ?: -1
                        val usernameFromDb = it[UserEntity.username] ?: ""
                        val passwordFromDb = it[UserEntity.password] ?: ""
                        User(id, usernameFromDb, passwordFromDb)
                    }.firstOrNull()

                if (user == null) {
                    val response = Response(
                        data = "Invalid Username or Password",
                        successful = false
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                val doesPasswordMatch = BCrypt.checkpw(password, user.password)

                if (!doesPasswordMatch) {
                    val response = Response(
                        data = "Invalid Username or Password",
                        successful = false
                    )
                    call.respond(HttpStatusCode.BadRequest, response)
                    return@post
                }

                val token = tokenManager.generateJWTToken(user)
                val response = Response(token, true)
                call.respond(HttpStatusCode.OK, response)

            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response(
                    data = "Failed to log in",
                    successful = false
                )
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        authenticate {
            get("/me") {
                val principle = call.principal<JWTPrincipal>()
                val username = principle?.payload?.getClaim("username")?.asString() ?: ""
                val userId = principle?.payload?.getClaim("id")?.asInt() ?: -1
                call.respondText("$username and $userId")
            }
        }
    }
}