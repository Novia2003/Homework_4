package ru.tbank

import io.ktor.client.engine.cio.CIO
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking
import ru.tbank.dsl.newsPrinter
import ru.tbank.dto.NewsDTO
import ru.tbank.service.NewsService
import ru.tbank.utils.getMostRatedNews

fun main() = runBlocking {
    val newsService = NewsService(CIO.create())

    val (mostRatedNews: List<NewsDTO>, duration: Duration) = measureTimedValue {
        val newsList = newsService.getNews(count = 100)

        val mostRatedNews = newsList.getMostRatedNews(
            10,
            LocalDate.of(2024, 1, 1)..LocalDate.of(2024, 12, 31)
        )
        newsService.saveNews("news.csv".toNewsResourcePath(), mostRatedNews)

        mostRatedNews
    }

    val output = newsPrinter {
        header(level = 1) { append("Most Rated News") }

        mostRatedNews.forEach { news ->
            news(news)
        }
    }

    println(output.build())

    output.saveToFile("news.md".toNewsResourcePath())

    println("The process of saving news before parallelization was performed ${duration.inWholeNanoseconds} in nanoseconds.")


    val duration2 = measureTimedValue {
        newsService.getNewsAndSaveThemUsingCoroutines(path = "news2.csv".toNewsResourcePath())
    }

    print("The process of saving news after parallelization was performed ${duration2.duration.inWholeNanoseconds} in nanoseconds.")

}

private fun String.toNewsResourcePath() =
    "src/main/resources/news/$this"