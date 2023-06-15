package com.example.routing

import com.example.db.DatabaseConnection
import com.example.entities.NoteEntity
import com.example.model.Note
import com.example.model.NoteRequest
import com.example.model.Response
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.*


fun Application.notesRoutes() {
    val db = DatabaseConnection.database

    routing {

        get("/notes") {
            try {
                val notes = db.from(NoteEntity).select().map { row: QueryRowSet ->
                    val id = row[NoteEntity.id] ?: -1
                    val content = row[NoteEntity.content] ?: ""
                    Note(id, content)
                }
                val response = Response(notes, true)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to get notes", false)
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        get("/notes/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: -1

                val note = db.from(NoteEntity)
                    .select()
                    .where { NoteEntity.id eq id }
                    .map { row ->
                        val noteId = row[NoteEntity.id] ?: -1
                        val content = row[NoteEntity.content] ?: ""
                        Note(noteId, content)
                    }.firstOrNull()

                if (note != null) {
                    val response = Response(note, true)
                    call.respond(HttpStatusCode.Found, response)
                } else {
                    val response = Response("Note not found", false)
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to get note", false)
                call.respond(HttpStatusCode.NotFound, response)
            }
        }

        post("/notes") {
            try {
                val noteRequest = call.receive<NoteRequest>()

                val noteId = db.insertAndGenerateKey(NoteEntity) { entity ->
                    set(entity.content, noteRequest.content)
                } as Int

                val note = Note(noteId, noteRequest.content)

                val response = Response(note, true)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to create note", false)
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        put("/notes/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: -1
                val noteRequest = call.receive<NoteRequest>()

                val rowsEffected = db.update(NoteEntity) { noteEntity ->
                    set(noteEntity.content, noteRequest.content)
                    where {
                        noteEntity.id eq id
                    }
                }

                if (rowsEffected == 1) {
                    val note = Note(id, noteRequest.content)
                    val response = Response(note, true)
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    val response = Response("Failed to update note", false)
                    call.respond(HttpStatusCode.NotFound, response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to update note", false)
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

        delete("/notes/{id}") {
            try {
                val id = call.parameters["id"]?.toInt() ?: -1

                val rowsEffected = db.delete(NoteEntity) {
                    it.id eq id
                }

                if (rowsEffected == 1) {
                    val response = Response("Note deleted successfully", true)
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    val response = Response("Failed to delete note", false)
                    call.respond(HttpStatusCode.BadRequest, response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val response = Response("Failed to delete note", false)
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }

    }
}