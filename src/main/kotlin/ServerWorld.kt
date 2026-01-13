import Opcode.C_HANDSHAKE
import Opcode.C_LOGIN
import Opcode.C_LOGIN_INIT
import Opcode.C_LOGIN_RE_INIT
import ServerOnDemand.CrcTable
import ServerOnDemand.handleOnDemandSocket
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import rs.Environment
import rs.io.Packet
import java.math.BigInteger
import java.util.*

object ServerWorld {

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private val sessions = mutableMapOf<String, Client>()
    val loginBuf = Packet.alloc(1)

    var nextTick = 0L

    fun run(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val serverGame = aSocket(selectorManager).tcp().bind("0.0.0.0", 43594)
            println("[:43594] World listening")

            while (true) {
                val socket = serverGame.accept()
                println("[:43594] Game connection from: ${socket.remoteAddress}")
                launch { handleClient(socket) }
            }
        }

        scope.launch(Dispatchers.Default) {
            while (isActive) {
                val tickStart = System.currentTimeMillis()

                cycle()

                val elapsed = System.currentTimeMillis() - tickStart
                val remaining = Environment.TICK_RATE - elapsed

                if (remaining > 0) delay(remaining)
                else println("⏱ MISSED TICK by ${-remaining}ms!")
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

                println("[Login] (Passed CRCs / RSA)")
            }

            C_HANDSHAKE -> handshake(client)

            else -> {
                println("Unhandled opcode: ${client.opcode}")
                withContext(Dispatchers.IO) { client.socket.close() }
            }
        }

        client.opcode = -1
    }

    private suspend fun cycle() {
        nextTick = System.currentTimeMillis() + Environment.TICK_RATE
    }
}
