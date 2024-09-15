package ru.tbank.utils

import org.slf4j.LoggerFactory
import ru.tbank.dto.News
import java.time.LocalDate

fun List<News>.getMostRatedNews(count: Int, period: ClosedRange<LocalDate>): List<News> {
    val logger = LoggerFactory.getLogger(this::class.java)

    logger.info("Filtering and sorting news by rating within period: $period")

    return try {
        val filteredNews = mutableListOf<News>()

        for (news in this) {
            val publicationDate = news.date

            if (publicationDate != null && publicationDate in period) {
                filteredNews.add(news)
            }
        }

        logger.info("Filtered news count: ${filteredNews.size}")

        val sortedNews = filteredNews
            .sortedByDescending { it.rating }
            .take(count)

        logger.info("Top ${sortedNews.size} most rated news within the period: $period")
        sortedNews
    } catch (e: Exception) {
        logger.error("Error processing news list", e)
        emptyList()
    }
}