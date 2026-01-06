package rs.engine

import Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rs.io.FileStream
import rs.io.Packet
import java.io.File
import java.util.Timer
import java.util.TimerTask

/**
 * TODO
 */
object OnDemand {

    val cache =  FileStream(File("./data/pack/").toPath())

    data class OnDemandRequest(
        val client: Client,
        val archive: Int,
        val file: Int
    )

    val urgentRequests = ArrayList<OnDemandRequest>()   // needed ASAP
    val extraRequests = ArrayList<OnDemandRequest>()    // pre-login extras
    val ingameRequests = ArrayList<OnDemandRequest>()   // in-game extras

    private val timer = Timer("OnDemand", true)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                withContext(NonCancellable) {
                    cycle()
                }
                delay(50L)
            }
        }
    }



    private suspend fun cycle() {
        // TODO: limit requests per client per cycle

        var i = 0
        while (i < urgentRequests.size) {
            val req = urgentRequests[i]
            send(req.client, req.archive, req.file)
            urgentRequests.removeAt(i)
        }

        i = 0
        while (i < extraRequests.size) {
            val req = extraRequests[i]
            send(req.client, req.archive, req.file)
            extraRequests.removeAt(i)
        }

        i = 0
        while (i < ingameRequests.size) {
            val req = ingameRequests[i]
            send(req.client, req.archive, req.file)
            ingameRequests.removeAt(i)
        }
    }

    //TODO: Luna doesn't handle pre login states
    fun onClientData(client: Client) {
        if (client.state != 2) return
        if (client.available < 4) return

        val buf = Packet.alloc(0)

        while (client.available >= 4) {
            //client.read(buf.data, 0, 4)
            buf.position(0)

            val archive = buf.g1()
            val file = buf.g2()
            val priority = buf.g1()

            if (archive > 3 || priority > 2) {
                client.close()
                return
            }

            val req = OnDemandRequest(client, archive, file)
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
                var remaining = req.size - pos
                if (remaining > 500) {
                    remaining = 500
                }

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
            // rejected if size = 0
            val temp = Packet(ByteArray(6))
            temp.p1(archive)
            temp.p2(file)
            temp.p2(0)
            temp.p1(0)
            client.send(temp.data)
        }
    }
}
