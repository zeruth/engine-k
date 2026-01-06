import Opcode.C_HANDSHAKE
import Opcode.C_LOGIN
import Opcode.C_LOGIN_INIT
import Opcode.C_LOGIN_RE_INIT
import RS2KtorServer.selectorManager
import RS2KtorServer.sessions
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.availableForRead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import rs.engine.OnDemand
import rs.io.Packet
import java.util.UUID

object ServerWorld {
    fun run(scope: CoroutineScope) {
        scope.launch {
            val serverGame = aSocket(selectorManager).tcp().bind("0.0.0.0", 43594)
            println("RS2 Game Server listening on port 43594")
            while (true) {
                val socket = serverGame.accept()
                println("[:43594] Game connection from: ${socket.remoteAddress}")
                launch { connection(socket) }
            }
        }
    }

    suspend fun connection(socket: Socket) {
        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        val client = Client(
            sessionId = UUID.randomUUID().toString(),
            socket = socket,
            input = input,
            output = output
        )

        client.fill()

        handle(client)
    }

    suspend fun handle(client: Client) {
        while (true) {
            if (client.socket.isClosed) return

            try {
                if (client.state == 0) {
                    handleWorld(client);
                } else {
                    handleOnDemand(client);
                }
            } catch (e: Exception) {
                e.printStackTrace()
                client.close()
            }
        }
    }

    val loginBuf = Packet.alloc(1)

    suspend fun handleWorld(client: Client) {
        println("[HandleWorld]")
        sessions[client.sessionId] = client

        if (client.available < 1) {
            client.fill()
            return
        }

        if (client.opcode == -1) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 1)

            client.opcode = loginBuf.g1()
            println("[Error] opcode ${client.opcode}")

            if (client.opcode == C_LOGIN) {
                client.waiting = 1;
            } else if (client.opcode == C_LOGIN_INIT || client.opcode == C_LOGIN_RE_INIT) {
                client.waiting = -1;
            } else {
                client.waiting = 0;
            }
        }

        if (client.waiting == -1) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 1);

            client.waiting = loginBuf.g1();
        } else if (client.waiting == -2) {
            loginBuf.position(0)
            client.read(loginBuf.data, 0, 2);

            client.waiting = loginBuf.g2();
        }

        if (client.available < client.waiting) {
            return;
        }

        loginBuf.position(0)
        client.read(loginBuf.data, 0, client.waiting);

        if (client.opcode == C_LOGIN) {
            client.state = 0;
            loginHandshake(client)
        } else if (client.opcode == C_LOGIN_INIT || client.opcode == C_LOGIN_RE_INIT) {
            var rev = loginBuf.g1()
            if (rev == 255)
                rev = loginBuf.g2()
            println("Login: Revision $rev")

            val lowMem = loginBuf.g1() == 1

            val crc_0 = loginBuf.g4()
            val crc_1 = loginBuf.g4()
            val crc_2 = loginBuf.g4()
            val crc_3 = loginBuf.g4()
            val crc_4 = loginBuf.g4()
            val crc_5 = loginBuf.g4()
            val crc_6 = loginBuf.g4()
            val crc_7 = loginBuf.g4()
        } else if (client.opcode == C_HANDSHAKE) {
            handshake(client)
        } else {
            client.socket.close();
        }

        client.opcode = -1
    }

    suspend fun handshake(client: Client) {
        client.state = 2;
        client.send(ByteArray(Int.SIZE_BYTES * 2))
        println("[Handshake]")
    }

    suspend fun loginHandshake(client: Client) {
        client.send(ByteArray(Int.SIZE_BYTES * 2))
        client.send(ByteArray(Byte.SIZE_BYTES))

        val seed = Packet(ByteArray(Int.SIZE_BYTES * 2))

        seed.p4((Math.random() * 0x00FFFFFF).toInt())
        seed.p4((Math.random() * 0xFFFFFFFFL).toInt())

        client.send(seed.data)
        println("[Login]")
    }

    suspend fun handleOnDemand(client: Client) {
        if (client.state != 2)
            return

        if (client.input.availableForRead < Int.SIZE_BYTES) {
            client.fill()
            return
        }

        val buf = Packet.alloc(0)
        while (client.input.availableForRead >= Int.SIZE_BYTES) {
            client.read(buf.data, 0, Int.SIZE_BYTES);

            val archive = buf.g1()
            val file = buf.g2()
            val priority = buf.g1()

            if (archive > 3 || priority > 2) {
                println("closed")
                client.close()
            }

            val request = OnDemand.OnDemandRequest(client, archive, file)

            if (priority == 2) {
                OnDemand.urgentRequests.add(request)
            } else if (priority == 1) {
                OnDemand.extraRequests.add(request)
            } else {
                OnDemand.ingameRequests.add(request)
            }
        }
    }
}