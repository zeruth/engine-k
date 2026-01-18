package rs.net.msg.out.game

import kotlinx.coroutines.runBlocking
import rs.engine.entity.Player
import rs.io.Packet

open class ServerGameMessage(val priority: ServerGameProtPriority,
                             val prot: ServerGameProt? = null,
                             val encode: (Packet.() -> Unit)? = null) {
    companion object {
        fun ServerGameMessage.write(player: Player) {
            prot ?: return
            encode ?: throw RuntimeException("encode is null")
            val client = player.client ?: return

            val buf = client.buffer

            buf.position(0)

            if (client.encryptor != null) {
                buf.p1(prot.id + client.encryptor!!.nextInt())
            } else {
                buf.p1(prot.id)
            }

            if (prot.length == -1) {
                buf.p1(0)
            } else if (prot.length == -2) {
                buf.p2(0)
            }

            val start = buf.position()
            buf.encode()

            if (prot.length == -1) {
                buf.psize1(buf.position() - start)
            } else if (prot.length == -2) {
                buf.psize2(buf.position() - start)
            }

            val slice = buf.data.sliceArray(0 until buf.position())

            runBlocking {
                client.send(slice)
            }
        }
    }
}