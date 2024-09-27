package ru.tbank.dto

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsDTO(
    val id: Long,
    @SerialName("publication_date")
    val publicationDate: Long,
    val title: String,
    val place: PlaceDTO? = null,
    val description: String,
    @SerialName("site_url")
    val siteUrl: String,
    @SerialName("favorites_count")
    val favoritesCount: Long,
    @SerialName("comments_count")
    val commentsCount: Long
) {
    val rating: Double by lazy {
        1 / (1 + exp(-(favoritesCount.toDouble() / (commentsCount.toDouble() + 1))))
    }

    val date: LocalDate?
        get() {
            return publicationDate.let {
                val instant = Instant.ofEpochSecond(it)
                val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                zonedDateTime.toLocalDate()
            }
        }
}