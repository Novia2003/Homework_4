package ru.tbank

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import ru.tbank.dsl.newsPrinter
import ru.tbank.service.NewsService
import ru.tbank.utils.getMostRatedNews

fun main() = runBlocking {
    val newsService = NewsService()

    val newsList = newsService.getNews(100)
    val mostRatedNews = newsList.getMostRatedNews(
        10,
        LocalDate.of(2024, 1, 1)..LocalDate.of(2024, 12, 31)
    )
    newsService.saveNews("news.csv".toNewsResourcePath(), mostRatedNews)

    val output = newsPrinter {
        header(level = 1) { append("Most Rated News") }

        mostRatedNews.forEach { news ->
            news(news)
        }
    }

    println(output.build())

    output.saveToFile("news.md".toNewsResourcePath())
}

private fun String.toNewsResourcePath() =
    "src/main/resources/news/$this"