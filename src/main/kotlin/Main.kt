package ru.tbank

import ru.tbank.dsl.newsPrinter
import ru.tbank.service.NewsService
import ru.tbank.utils.getMostRatedNews
import java.time.LocalDate

suspend fun main() {
    val newsService = NewsService()

    val newsList = newsService.getNews(100)
    val mostRatedNews = newsList.getMostRatedNews(10, LocalDate.of(2024, 1, 1)..LocalDate.of(2024, 12, 31))
    newsService.saveNews("src/main/resources/news/news.csv", mostRatedNews)

    val output = newsPrinter {
        header(level = 1) { append("Most Rated News") }

        mostRatedNews.forEach { news ->
            news(news)
        }
    }

    println(output.build())

    output.saveToFile("src/main/resources/news/news.md")
}