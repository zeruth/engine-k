package rs.net

import io.ktor.network.sockets.Socket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import java.util.UUID

class Client(
    val sessionId: String,
    val socket: Socket,
    val input: ByteReadChannel,
    val output: ByteWriteChannel
) {
    val uuid = UUID.randomUUID().toString()
    var state = 0
    var opcode = -1
    var waiting = 0
    val available: Int
        get() = input.availableForRead

    lateinit var decryptor: Isaac
    lateinit var encryptor: Isaac

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