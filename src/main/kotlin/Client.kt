import io.ktor.network.sockets.Socket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully

class Client(
    val sessionId: String,
    val socket: Socket,
    val input: ByteReadChannel,
    val output: ByteWriteChannel
) {
    var state = 0
    var opcode = -1
    var waiting = 0
    val available: Int
        get() = input.availableForRead

    /**
     * readFully will suspend until there are enough bytes to be read
     */
    suspend fun read(dst: ByteArray, offset: Int, length: Int) {
        input.readFully(dst, offset, length)
    }

    suspend fun send(data: ByteArray) {
        output.writeFully(data)
    }

    fun close() {
        try { socket.close() } catch (_: Exception) {}
        state = -1
    }
}