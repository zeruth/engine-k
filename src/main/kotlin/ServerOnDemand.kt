import RS2KtorServer.selectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.EOFException
import rs.io.FileStream
import rs.io.Packet
import java.io.File

object ServerOnDemand {

    data class OnDemandRequest(val client: Client, val archive: Int, val file: Int, val priority: Int)

    val CrcBuffer = Packet(ByteArray(40))
    val CrcTable = Array<Int?>(9) { null }
    var CrcBuffer32 = 0

    val cache = FileStream(File("./data/pack/").toPath())

    val urgentRequests = mutableListOf<OnDemandRequest>()
    val extraRequests = mutableListOf<OnDemandRequest>()
    val ingameRequests = mutableListOf<OnDemandRequest>()

    init {
        val count = cache.count(0)
        for (i in 0 until count) {
            val jag = cache.read(0, i)
            val crc = jag?.let { Packet.getcrc(it, 0, it.size) } ?: 0
            CrcTable[i] = crc
            CrcBuffer.p4(crc)
        }

        var hash = 1234
        for (i in 0 until 9) hash = (hash shl 1) + (CrcTable[i] ?: 0)
        CrcBuffer.p4(hash)
        CrcBuffer32 = Packet.getcrc(CrcBuffer.data, 0, CrcBuffer.data.size)
    }

    private val archiveMap: Map<String, ByteArray?> by lazy {
        mapOf(
            "/crc" to CrcBuffer.data,
            "/title" to cache.read(0, 1),
            "/config" to cache.read(0, 2),
            "/interface" to cache.read(0, 3),
            "/media" to cache.read(0, 4),
            "/versionlist" to cache.read(0, 5),
            "/textures" to cache.read(0, 6),
            "/wordenc" to cache.read(0, 7),
            "/sounds" to cache.read(0, 8)
        )
    }

    fun runHttp(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val serverJS5 = aSocket(selectorManager).tcp().bind("0.0.0.0", 80)
            println("[:80 :43594] OnDemand listening")
            while (true) {
                val socket = serverJS5.accept()
                launch { handleOnDemandHTTP(socket) }
            }
        }
    }

    fun run(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            println("[:43594] OnDemand Serving")

            while (true) {
                val tickStart = System.currentTimeMillis()

                cycle()

                val elapsed = System.currentTimeMillis() - tickStart
                val remaining = 50 - elapsed

                if (remaining > 0) delay(remaining)
                else println("[OnDemand] MISSED TICK by ${-remaining}ms!")
            }
        }
    }


    suspend fun handleOnDemandHTTP(socket: Socket) = withContext(Dispatchers.IO) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)

        try {
            val requestLine = input.readUTF8Line(1024) ?: return@withContext
            var path = requestLine.split(" ").getOrNull(1) ?: "/"
            path = path.replace("-", "").takeWhile { !it.isDigit() }

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
        } catch (e: Exception) {
            println("JS5 HTTP Error: ${e.message}")
        } finally {
            socket.close()
        }
    }

    suspend fun handleOnDemandSocket(client: Client) {
        val buf = Packet.alloc(4)

        while (true) {
            try {
                client.read(buf.data, 0, 4)
            } catch (e: EOFException) {
                client.close()
                return
            }

            buf.position(0)

            val archive = buf.g1()
            val file = buf.g2()
            val priority = buf.g1()

            if (archive > 3 || priority > 2) {
                client.close()
                return
            }

            val req = OnDemandRequest(client, archive, file, priority)
            when (priority) {
                2 -> urgentRequests.add(req)
                1 -> extraRequests.add(req)
                else -> ingameRequests.add(req)
            }
        }
    }


    private suspend fun send(client: Client, archive: Int, file: Int) {
        val req = cache.read(archive + 1, file)
        if (req != null) {
            var pos = 0
            var part = 0
            while (pos < req.size) {
                val remaining = minOf(500, req.size - pos)
                val temp = Packet(ByteArray(6 + remaining))
                temp.p1(archive)
                temp.p2(file)
                temp.p2(req.size)
                temp.p1(part)
                temp.pdata(req, pos, remaining)

                pos += remaining
                part++
                client.send(temp.data)
            }
        } else {
            val temp = Packet(ByteArray(6))
            temp.p1(archive)
            temp.p2(file)
            temp.p2(0)
            temp.p1(0)
            client.send(temp.data)
        }
    }

    /**
     * Processes on-demand queues with a 50ms per-tick budget,
     * limiting requests per client to avoid flooding.
     */
    suspend fun cycle() = withTimeoutOrNull(50) {
        val MAX_PER_CLIENT = 1000
        val sentPerClient = mutableMapOf<Client, Int>()

        fun canSend(client: Client): Boolean {
            val count = sentPerClient.getOrDefault(client, 0)
            if (count >= MAX_PER_CLIENT) return false
            sentPerClient[client] = count + 1
            return true
        }

        suspend fun processQueue(queue: MutableList<OnDemandRequest>) {
            val snapshot = queue.toList()
            queue.clear()
            for (req in snapshot) {
                if (!canSend(req.client)) continue
                try { send(req.client, req.archive, req.file) }
                catch (_: Exception) { }
            }
        }

        processQueue(urgentRequests)
        processQueue(extraRequests)
        processQueue(ingameRequests)
    }
}
