package ru.tbank.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.Semaphore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.tbank.dto.NewsDTO
import ru.tbank.dto.ResponseDTO

class NewsService(engine: HttpClientEngine) {

    private val client = HttpClient(engine)

    private val logger = LoggerFactory.getLogger(NewsService::class.java)

    private val semaphore = Semaphore(MAX_CONCURRENT_REQUEST)

    suspend fun getNews(page: Int = 1, count: Int = 100): List<NewsDTO> {
        return try {
            logger.info("Fetching news with count: $count")

            withContext(Dispatchers.IO) {
                semaphore.acquire()
            }

            val response: HttpResponse = client.get(URL) {
                parameter("page", page)
                parameter("page_size", count)
                parameter("order_by", "date")
                parameter("location", "spb")
                parameter(
                    "fields",
                    "id,publication_date,title,place,description,site_url,favorites_count,comments_count"
                )
            }

            val json = Json {
                ignoreUnknownKeys = true
            }

            if (response.status.isSuccess()) {
                logger.info("Successfully fetched news")
                val newsResponse = json.decodeFromString<ResponseDTO>(response.bodyAsText())

                newsResponse.results
            } else {
                logger.error("Failed to fetch news. Status: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error fetching news", e)
            emptyList()
        } finally {
            client.close()
            semaphore.release()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun getNewsAndSaveThemUsingCoroutines(
        quantityWorkers: Int = 5,
        threadPoolSize: Int = 3,
        quantityPages: Int = 10,
        pageSize: Int = 10,
        path: String
    ) {
        val workerDispatcher = newFixedThreadPoolContext(threadPoolSize, "workerPool")
        val channel = Channel<List<NewsDTO>>(Channel.UNLIMITED)

        coroutineScope {
            val workers = (0 until quantityWorkers).map { workerIndex ->
                launch(workerDispatcher) {
                    var pageIndex = workerIndex

                    while (pageIndex < quantityPages) {
                        val news = getNews(pageIndex + 1, pageSize)
                        channel.send(news)

                        logger.info("Worker $workerIndex fetched news for page ${pageIndex + 1}")
                        pageIndex += quantityWorkers
                    }
                }
            }

            val writerJob = launch(Dispatchers.IO) {
                try {
                    val file = File(path)

                    BufferedWriter(FileWriter(file)).use { writer ->
                        for (newsList in channel) {
                            newsList.forEach { news ->
                                writer.write(
                                    "${news.id}," +
                                        "${news.date}," +
                                        "${news.title}," +
                                        "${news.place}," +
                                        "${news.description}," +
                                        "${news.siteUrl}," +
                                        "${news.favoritesCount}," +
                                        "${news.commentsCount}," +
                                        "${news.rating}\n"
                                )

                                logger.info("Saved news: ${news.title}")
                            }
                        }
                    }
                } catch (e: IOException) {
                    logger.error("Failed to save news to $path", e)
                }
            }

            workers.forEach { it.join() }
            channel.close()
            writerJob.join()
        }

        logger.info("All news have been saved to $path")
    }

    fun saveNews(path: String, news: Collection<NewsDTO>) {
        if (path.isEmpty()) {
            logger.warn("File path is null or empty.")
            return
        }

        val file = File(path)
        logger.info("Attempting to save news to $path")

        if (file.exists()) {
            logger.error("File already exists at path: $path")
            throw IllegalArgumentException("File already exists at path: $path")
        }

        try {
            file.bufferedWriter().use { writer ->
                writer.write("id,date,title,place,description,siteUrl,favoritesCount,commentsCount,rating\n")

                news.forEach { news ->
                    writer.write(
                        "${news.id}," +
                                "${news.date}," +
                                "${news.title}," +
                                "${news.place}," +
                                "${news.description}," +
                                "${news.siteUrl}," +
                                "${news.favoritesCount}," +
                                "${news.commentsCount}," +
                                "${news.rating}\n"
                    )
                }
            }

            logger.info("News saved to $path")
        } catch (e: IOException) {
            logger.error("Failed to save news to $path", e)
        }
    }

    private companion object {
        private const val URL = "https://kudago.com/public-api/v1.4/news/"

        private const val MAX_CONCURRENT_REQUEST = 5
    }
}