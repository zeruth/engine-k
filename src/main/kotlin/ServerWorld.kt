import rs.net.Opcode.C_HANDSHAKE
import rs.net.Opcode.C_LOGIN
import rs.net.Opcode.C_LOGIN_INIT
import rs.net.Opcode.C_LOGIN_RE_INIT
import ServerOnDemand.CrcTable
import ServerOnDemand.handleOnDemandSocket
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import rs.Environment
import rs.engine.World
import rs.io.Packet
import rs.net.Client
import rs.net.Isaac
import rs.net.RSA
import rsmod.PathFinder
import util.Logger
import java.math.BigInteger
import java.util.*
import java.util.concurrent.Executors

object ServerWorld {
    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val socketBuilder = aSocket(selectorManager).tcp()
    var server: ServerSocket

    private val sessions = mutableMapOf<String, Client>()
    val loginBuf = Packet.alloc(1)

    init {
        runBlocking {
            World // calling World here guarantees it loads before the first tick fires
            server = socketBuilder.bind("0.0.0.0", 43594)

            Logger.messageColor = Logger.Color.YELLOW
            Logger.info("World", "[:43594] listening")
        }
    }

    val worldDispatcher = Executors
        .newSingleThreadExecutor()
        .asCoroutineDispatcher()


    fun run(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                val socket = server.accept()
                Logger.messageColor = Logger.Color.YELLOW
                Logger.info("World", "[:43594] connection from: ${socket.remoteAddress}")
                launch { handleClient(socket) }
            }
        }

        /**
         * ServerWorld loop running on [ExecutorCoroutineDispatcher].
         *
         * This coroutine:
         * 1. Calculates the absolute expected start time for each tick.
         * 2. Sleeps most of the interval, then spins to align precisely.
         * 3. Measures drift between expected and actual start times.
         * 4. Updates statistics: total drift, max drift, missed ticks.
         * 5. Calls [World.cycle()] to advance game state.
         * 6. Logs periodic tick statistics every ~10 seconds.
         *
         * Drift is automatically corrected each tick.
         * Late ticks do not accumulate.
         */
        scope.launch(worldDispatcher) {
            val tickIntervalMs = 600L
            val tickIntervalNs = tickIntervalMs * 1_000_000

            val startTime = System.nanoTime()
            var tickCount = 0L

            var totalDriftNs = 0L
            var maxDriftNs = 0L
            var missedTicks = 0L

            while (isActive) {
                val expectedTime = startTime + tickCount * tickIntervalNs
                val freeNs = expectedTime - System.nanoTime()
                val freeMs = freeNs / 1_000_000
                if (freeMs > 16) {
                    delay(freeMs - 15)
                }

                while (System.nanoTime() < expectedTime) {
                    Thread.onSpinWait()
                }

                val actualStart = System.nanoTime()
                val driftNs = actualStart - expectedTime

                totalDriftNs += driftNs
                maxDriftNs = maxOf(maxDriftNs, kotlin.math.abs(driftNs))

                if (driftNs > tickIntervalNs) {
                    missedTicks += driftNs / tickIntervalNs
                }

                World.cycle()

                tickCount++

                if (tickCount % (10_000 / tickIntervalMs) == 0L) {
                    Logger.messageColor = Logger.Color.CYAN
                    Logger.info(
                        "World",
                        buildString {
                            append("TickStats | ")
                            append("ticks=$tickCount, ")
                            append("avgDrift=${totalDriftNs / tickCount / 1_000_000}ms, ")
                            append("maxDrift=${maxDriftNs / 1_000_000}ms, ")
                            append("missedTicks=$missedTicks")
                        }
                    )
                }
            }
        }
    }


    private suspend fun handleClient(socket: Socket) {
        val client = Client(
            sessionId = UUID.randomUUID().toString(),
            socket = socket,
            input = socket.openReadChannel(),
            output = socket.openWriteChannel(autoFlush = true)
        )

        sessions[client.sessionId] = client

        try {
            while (client.state != -1 && !socket.isClosed) {
                when (client.state) {
                    0 -> handleWorldSocket(client)
                    else -> handleOnDemandSocket(client)
                }
            }
        } catch (_: Exception) {
            client.close()
        } finally {
            sessions.remove(client.sessionId)
        }
    }

    suspend fun handshake(client: Client) {
        client.state = 2
        client.send(ByteArray(8))
    }

    suspend fun loginHandshake(client: Client) {
        client.send(ByteArray(Int.SIZE_BYTES * 2))
        client.send(ByteArray(Byte.SIZE_BYTES))

        val seed = Packet(ByteArray(Int.SIZE_BYTES * 2))
        seed.p4((Math.random() * 0x00FFFFFF).toInt())
        seed.p4((Math.random() * 0xFFFFFFFFL).toInt())
        client.send(seed.data)
    }

    suspend fun Client.respondOutOfDate() {
        send(byteArrayOf(6))
        close()
    }

    var loginRequests = HashMap<String, Client>()

    private suspend fun handleWorldSocket(client: Client) {
        if (client.opcode == -1) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 1)
            client.opcode = loginBuf.g1()
            client.waiting = when (client.opcode) {
                C_LOGIN -> 1
                C_LOGIN_INIT, C_LOGIN_RE_INIT -> -1
                else -> 0
            }
        }

        if (client.waiting == -1) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 1)
            client.waiting = loginBuf.g1()
        } else if (client.waiting == -2) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 2)
            client.waiting = loginBuf.g2()
        }

        loginBuf.position(0)
        client.read(loginBuf.data, 0, client.waiting)

        when (client.opcode) {
            C_LOGIN -> {
                client.state = 0
                loginHandshake(client)
            }

            C_LOGIN_INIT, C_LOGIN_RE_INIT -> {
                var rev = loginBuf.g1()
                if (rev == 255) rev = loginBuf.g2()

                if (rev != Environment.ENGINE_REVISION) {
                    client.respondOutOfDate()
                    return
                }

                val lowMem = loginBuf.g1() == 1
                val crcs = IntArray(CrcTable.size) { loginBuf.g4s() }

                if (!crcs.withIndex().all { (i, crc) -> crc == CrcTable[i] }) {
                    client.respondOutOfDate()
                    return
                }

                val loginBufSize = loginBuf.g1()
                val enc = ByteArray(loginBufSize)
                loginBuf.gdata(enc, 0, enc.size)

                val plain = Packet(BigInteger(enc).modPow(RSA.privateExponent, RSA.privateModulus).toByteArray())

                loginBuf.position(0)
                loginBuf.pdata(plain.data, 0, plain.data.size)
                loginBuf.position(0)

                val opcode = loginBuf.g1()
                if (opcode != 10) {
                    client.respondOutOfDate()
                    return
                }

                val seed = IntArray(4)
                for (i in 0 until 4) {
                    seed[i] = loginBuf.g4s()
                }
                client.decryptor = Isaac(seed)
                for (i in 0 until 4) {
                    seed[i] += 50
                }
                client.encryptor = Isaac(seed)

                val uid = loginBuf.g4s()
                val username = loginBuf.gjstr()
                val password = loginBuf.gjstr()

                if (username.length !in 1..12) {
                    client.send(ByteArray(1) { 3 })
                    client.close()
                    return
                }

                if (password.length !in 1..20) {
                    client.send(ByteArray(1) { 3 })
                    client.close()
                    return
                }

                //TODO: max players check

                //TODO: check logout requests

                loginRequests[client.uuid] = client

                World.login(client, username, password)
            }

            C_HANDSHAKE -> handshake(client)

            else -> {
                println("Unhandled opcode: ${client.opcode}")
                withContext(Dispatchers.IO) { client.socket.close() }
            }
        }

        client.opcode = -1
    }

    fun computeUid(username37: BigInteger, pid: Int): Int {
        val mask = username37.and(BigInteger("1FFFFF", 16)).toLong()
        val combined = (mask shl 11) or (pid.toLong() and 0x7FF)
        return combined.toUInt().toInt()
    }
}
