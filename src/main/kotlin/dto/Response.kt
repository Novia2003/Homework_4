package ru.tbank.dto

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val results: List<News>
)
