package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class NoteRequest(
    val content: String
)
