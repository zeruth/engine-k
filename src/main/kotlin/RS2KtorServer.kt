import io.ktor.network.selector.*
import kotlinx.coroutines.*
import rs.engine.script.RuneScriptProvider

object RS2KtorServer {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        RuneScriptProvider.parse()
        ServerOnDemand.runHttp(this)
        ServerWorld.run(this)
    }
}