import io.ktor.network.selector.*
import kotlinx.coroutines.*
import rs.engine.OnDemand
import java.util.concurrent.ConcurrentHashMap

object RS2KtorServer {
    val sessions = ConcurrentHashMap<String, Client>()
    val selectorManager = ActorSelectorManager(Dispatchers.IO)

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        OnDemand
        ServerJS5.runHttp(this)
        ServerJS5.runSocket(this)
        ServerWorld.run(this)
    }
}