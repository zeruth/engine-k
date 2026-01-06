import RS2KtorServer.selectorManager
import ext.ByteArrayExt.buffer
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ServerJS5 {

    fun runHttp(scope: CoroutineScope) {
        scope.launch {
            val serverJS5 = aSocket(selectorManager).tcp().bind("0.0.0.0", 80)
            println("RS2 JS5 HTTP Server listening on port 80")
            while (true) {
                val socket = serverJS5.accept()
                println("[:80] JS5 connection from: ${socket.remoteAddress}")
                launch { handleJS5Http(socket) }
            }
        }
    }

    fun runSocket(scope: CoroutineScope) {
        scope.launch {
            val serverJS5 = aSocket(selectorManager).tcp().bind("0.0.0.0", 43595)
            println("RS2 JS5 Socket Server listening on port 43595")
            while (true) {
                val socket = serverJS5.accept()
                println("[:43595] JS5 connection from: ${socket.remoteAddress}")
                launch { handleJS5TCP(socket) }
            }
        }
    }

    val archiveMap = mapOf(
        "/crc" to ByteArray(40).buffer(ByteOrder.BIG_ENDIAN).apply {
            // Dummy CRCs
            putInt(0)
            putInt(-1515619061)
            putInt(315293377)
            putInt(1433713710)
            putInt(-212251727)
            putInt(1602406472)
            putInt(-1456673236)
            putInt(-2063599502)
            putInt(1123906948)

            putInt(1501264872)
        }.array(),
        "/title" to ByteArray(0),
        "/config" to ByteArray(0),
        "/interface" to ByteArray(0),
        "/media" to ByteArray(0),
        "/versionlist" to ByteArray(0),
        "/textures" to ByteArray(0),
        "/wordenc" to ByteArray(0),
        "/sounds" to ByteArray(0)
    )

    suspend fun handleJS5Http(socket: Socket) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        try {
            val requestLine = input.readUTF8Line() ?: return
            var path = requestLine.split(" ").getOrNull(1) ?: "/"
            path = path.takeWhile { !it.isDigit() }

            println("Client requested HTTP JS5 page: $path")

            val data = archiveMap[path] ?: ByteArray(0)

            val headers = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Length: ${data.size}\r\n")
                append("Content-Type: application/octet-stream\r\n")
                append("Connection: close\r\n")
                append("\r\n")
            }
            output.writeFully(headers.encodeToByteArray())
            output.writeFully(data)
            println("Sent ${data.size} bytes for $path")
        } catch (e: Exception) {
            println("Error handling JS5 client: ${e.message}")
        } finally {
            socket.close()
        }
    }

    suspend fun handleJS5TCP(socket: Socket) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        try {

        } catch (e: Exception) {
            println("Error handling JS5 client: ${e.message}")
        }
    }
}