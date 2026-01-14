package util

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Logger {

    enum class Level(val priority: Int) {
        TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4)
    }

    object Color {
        const val RESET = "\u001B[0m"
        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val PURPLE = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"
    }

    // Global log level
    var globalLevel: Level = Level.INFO

    // Colors
    var timeColor: String? = null
    var topicColor: String? = null
    var messageColor: String? = null
    var timeBracketColor: String? = Color.GREEN
    var topicBracketColor: String? = Color.GREEN
    var messageBracketColor: String? = Color.GREEN

    // Timestamp formatting
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val TOPIC_WIDTH = 20 // adjust as needed

    private fun formatWithColor(text: String, color: String?) = color?.let { "$it$text${Color.RESET}" } ?: text

    private fun log(level: Level, topic: String, message: String) {
        if (level.priority < globalLevel.priority) return

        val timeStr = LocalTime.now().format(timeFormatter)

        val coloredTime = formatWithColor(timeStr, timeColor)
        val coloredTopic = formatWithColor(topic, topicColor)
        val coloredMessage = formatWithColor(message, messageColor)

        val openTime = formatWithColor("[", timeBracketColor)
        val closeTime = formatWithColor("]", timeBracketColor)

        val openTopic = formatWithColor("[", topicBracketColor)
        val closeTopic = formatWithColor("]", topicBracketColor)

        val openMessage = formatWithColor("[", messageBracketColor)
        val closeMessage = formatWithColor("]", messageBracketColor)

        var finalTopic = "$openTopic$coloredTopic$closeTopic"
        finalTopic = finalTopic.padEnd(35)

        println("$openTime$coloredTime$closeTime $finalTopic $openMessage$coloredMessage$closeMessage")
    }

    fun trace(topic: String, message: String) = log(Level.TRACE, topic, message)
    fun debug(topic: String, message: String) = log(Level.DEBUG, topic, message)
    fun info(topic: String, message: String) = log(Level.INFO, topic, message)
    fun warn(topic: String, message: String) = log(Level.WARN, topic, message)
    fun error(topic: String, message: String) = log(Level.ERROR, topic, message)
}

// Example usage
fun main() {
    Logger.globalLevel = Logger.Level.TRACE

    Logger.timeColor = Logger.Color.CYAN
    Logger.topicColor = Logger.Color.YELLOW
    Logger.messageColor = Logger.Color.GREEN

    Logger.timeBracketColor = Logger.Color.BLUE
    Logger.topicBracketColor = Logger.Color.PURPLE
    Logger.messageBracketColor = Logger.Color.RED

    Logger.info("SQL", "Database 'dawn_auth' does not exist: Initialize now? (y)es (n)o:")
    Logger.info("TESTING", "Database 'dawn_auth' does not exist: Initialize now? (y)es (n)o:")
}
