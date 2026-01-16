import rs.net.Opcode.C_HANDSHAKE
import rs.net.Opcode.C_LOGIN
import rs.net.Opcode.C_LOGIN_INIT
import rs.net.Opcode.C_LOGIN_RE_INIT
import ServerOnDemand.CrcTable
import ServerOnDemand.handleOnDemandSocket
import db.login.DBLogin
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import rs.Environment
import rs.cache.config.CategoryType
import rs.cache.config.DbRowType
import rs.cache.config.DbTableType
import rs.cache.config.EnumType
import rs.cache.config.FloType
import rs.cache.config.HuntType
import rs.cache.config.IdkType
import rs.cache.config.InvType
import rs.cache.config.LocType
import rs.cache.config.MesAnimType
import rs.cache.config.NpcType
import rs.cache.config.ObjType
import rs.cache.config.ParamType
import rs.cache.config.SeqType
import rs.cache.config.SpotAnimType
import rs.cache.config.StructType
import rs.cache.config.VarBitType
import rs.cache.config.VarNpcType
import rs.cache.config.VarPlayerType
import rs.cache.config.VarSharedType
import rs.engine.World
import rs.engine.entity.Player
import rs.engine.entity.PlayerLoading
import rs.engine.script.RuneScriptProvider.loginScript
import rs.engine.script.RuneScriptRunner
import rs.engine.script.ScriptState
import rs.io.Packet
import rs.net.Client
import rs.net.Isaac
import rs.net.RSA
import util.Logger
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
            Logger.messageColor = Logger.Color.YELLOW
            Logger.info("World", "[:43594] listening")

            while (true) {
                val socket = serverGame.accept()
                Logger.messageColor = Logger.Color.YELLOW
                Logger.info("World", "[:43594] connection from: ${socket.remoteAddress}")
                launch { handleClient(socket) }
            }
        }

        scope.launch(Dispatchers.Default) {
            while (isActive) {
                val tickStart = System.currentTimeMillis()

                World.cycle()

                val elapsed = System.currentTimeMillis() - tickStart
                val remaining = World.TICKRATE - elapsed

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
