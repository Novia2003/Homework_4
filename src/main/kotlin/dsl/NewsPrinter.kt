package ru.tbank.dsl

import java.io.File
import java.io.IOException
import org.slf4j.LoggerFactory
import ru.tbank.dto.NewsDTO

class NewsPrinter {

    private val builder = StringBuilder()

    private val logger = LoggerFactory.getLogger(NewsPrinter::class.java)

    fun header(level: Int, init: StringBuilder.() -> Unit) {
        builder.append("#".repeat(level)).append(" ")
        builder.init()
        builder.append("\n\n")
    }

    private fun text(init: StringBuilder.() -> Unit) {
        builder.init()
        builder.append("\n\n")
    }

    private fun bold(text: String) = "**$text**"

    fun news(news: NewsDTO) {
        header(level = 3) { append(news.title) }

        val map = linkedMapOf(
            "Date:" to "${news.date}",
            "Description:" to news.description,
            "Site URL:" to news.siteUrl,
            "Favorites Count:" to "${news.favoritesCount}",
            "Comments Count:" to "${news.commentsCount}",
            "Rating:" to "${news.rating}",
        )

        for ((key, value) in map) {
            text { append(bold(key)).append(" ").append(value) }
        }
    }

    fun build(): String = builder.toString()

    fun saveToFile(path: String) {
        if (path.isEmpty()) {
            logger.warn("File path is null or empty.")
            return
        }

        val file = File(path)
        logger.info("Attempting to save news to $path")

        try {
            file.bufferedWriter().use { writer ->
                writer.write(builder.toString())
            }

            logger.info("News saved to $path")
        } catch (e: IOException) {
            logger.error("Failed to save news to $path", e)
        }
    }
}

fun newsPrinter(init: NewsPrinter.() -> Unit): NewsPrinter {
    val printer = NewsPrinter()
    printer.init()
    return printer
}
