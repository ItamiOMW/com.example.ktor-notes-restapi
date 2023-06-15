package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: Int,
    val content: String
)
