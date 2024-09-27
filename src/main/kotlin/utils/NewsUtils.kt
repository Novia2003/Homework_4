package ru.tbank.utils

import java.time.LocalDate
import ru.tbank.dto.NewsDTO

fun List<NewsDTO>.getMostRatedNews(count: Int, period: ClosedRange<LocalDate>): List<NewsDTO> {
    if (count < 0) return this

    return this
        .filter { news ->
            news.date?.let { period.contains(it) } ?: false
        }.sortedByDescending { it.rating }
        .take(count)
}