package ru.tbank.dto

import kotlinx.serialization.Serializable

@Serializable
data class ResponseDTO(
    val results: List<NewsDTO>
)
