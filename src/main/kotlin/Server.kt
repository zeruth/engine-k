import db.login.DBLoginBootstrap
import io.ktor.network.selector.*
import kotlinx.coroutines.*
import rs.engine.script.RuneScriptProvider

object Server {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)

    @JvmStatic
    fun main(args: Array<String>) {
        //TODO don't drop
        DBLoginBootstrap.DROP()

        DBLoginBootstrap.init()

        runBlocking {
            RuneScriptProvider.parse()
            ServerOnDemand.runHttp(this)
            ServerOnDemand.run(this)
            ServerWorld.run(this)
        }
    }
}