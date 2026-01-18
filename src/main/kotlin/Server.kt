import db.login.DBLoginBootstrap
import io.ktor.network.selector.*
import kotlinx.coroutines.*
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
import rs.engine.GameMap
import rs.engine.script.RuneScriptProvider
import rs.engine.script.RuneScriptRunner
import util.Logger

object Server {
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    val start = System.currentTimeMillis()

    init {
        Logger.messageColor = Logger.Color.PURPLE
        Logger.info("Engine","---Lost-City (377)---")

        initConfigs()

        GameMap.load()
        RuneScriptProvider.parse()
    }

    private fun initConfigs() {
        CategoryType
        DbRowType
        DbTableType
        EnumType
        FloType
        HuntType
        IdkType
        InvType
        LocType
        MesAnimType
        NpcType
        ObjType
        ParamType
        SeqType
        SpotAnimType
        StructType
        VarBitType
        VarNpcType
        VarPlayerType
        VarSharedType
    }

    @JvmStatic
    fun main(args: Array<String>) {
        DBLoginBootstrap.init()

        runBlocking {
            ServerOnDemand.runHttp(this)
            ServerOnDemand.run(this)
            ServerWorld.run(this)
        }
    }
}