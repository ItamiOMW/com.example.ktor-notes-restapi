package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val data: T,
    val successful: Boolean,
)
