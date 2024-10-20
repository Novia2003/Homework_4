package service

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import ru.tbank.service.NewsService

class NewsServiceTest {

    private val mockEngine = MockEngine { _ ->
        respond(
            content = ByteReadChannel(
                """
                        {
                            "results": [
                                {
                                    "id": 1,
                                    "publication_date": 100000,
                                    "title": "В турции пропал мальчик",
                                    "place":
                                        {
                                            "id": 1
                                        }
                                    "description": "Description",
                                    "site_url": "http://aboba.com",
                                    "favorites_count": 10,
                                    "comments_count": 5
                                }
                            ]
                        }
                    """.trimIndent()),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test successful response`() = runTest {
        val newsService = NewsService(mockEngine)

        val result = newsService.getNews(page = 1, count = 1)

        assertEquals(1, result.size)
        assertEquals("В турции пропал мальчик", result[0].title)

        assertNotNull(result[0].place)
        result[0].place?.let { assertEquals(1, it.id) }
    }

    @Test
    fun testSemaphoreLimitation() = runBlocking {
        val newsService = NewsService(mockEngine)

        val activeRequests = Channel<Int>(Channel.UNLIMITED)
        val maxConcurrentRequests = 5
        var runningRequests = 0
        var maxRunningRequests = 0

        val job = coroutineScope {
            val jobs = List(10) { _ ->
                launch {
                    withContext(Dispatchers.IO) {
                        activeRequests.send(1)
                        newsService.getNews(1, 1)
                        activeRequests.send(-1)
                    }
                }
            }
            jobs
        }

        launch {
            for (change in activeRequests) {
                runningRequests += change
                maxRunningRequests = maxOf(maxRunningRequests, runningRequests)
            }
        }

        job.joinAll()
        activeRequests.close()

        assert(maxConcurrentRequests >= maxRunningRequests)
    }
}

