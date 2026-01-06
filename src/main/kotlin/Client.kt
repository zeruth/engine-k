import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import java.nio.ByteBuffer

// --- Player session ---
data class Client(
    val sessionId: String,
    var username: String? = null,
    var loggedIn: Boolean = false,
    val socket: Socket,
    val input: ByteReadChannel,
    val output: ByteWriteChannel,
) {
    var state = 0
    var opcode = -1
    var waiting = 0

    fun close() {
        socket.close()
    }

    private val buffer = ByteArray(65536)
    var available = 0

    fun read(dest: ByteArray, offset: Int, length: Int): Boolean {
        if (available < length) return false

        System.arraycopy(buffer, 0, dest, offset, length)
        available -= length
        System.arraycopy(buffer, length, buffer, 0, available)

        return true
    }

    suspend fun fill(): Boolean {
        val read = input.readAvailable(buffer, available, buffer.size - available)
        if (read > 0) {
            available += read
            return true
        }
        if (read == -1) {
            close()
        }
        return false
    }

    suspend fun send(buf: ByteBuffer) {
        output.writeFully(buf)
    }

    suspend fun send(buf: ByteArray) {
        output.writeFully(buf)
    }
}