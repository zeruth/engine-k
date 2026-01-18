package rs.engine.script

import ServerOnDemand.cache
import me.filby.neptune.runescript.compiler.codegen.script.RuneScript
import me.filby.neptune.serverscript.compiler.ServerScriptCompilerCLI
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
import rs.engine.entity.Player
import rs.engine.script.test.FakeScriptFile
import rs.io.Packet
import util.Logger
import java.io.File
import java.math.BigInteger

/**
 * Decoder for Server RuneScript Binaries.
 * Reads script.dat and script.idx with lookup
 */
object RuneScriptProvider {

    fun RuneScript.target(): String {
        return "[$trigger,$name$]"
    }

    init {
/*        GlobalEventBus.subscribe<LoginEvent> {
            println(loginScript.name())
            val state = RuneScriptRunner.init(loginScript, it.payload.plr)
            val result = ScriptState.of(RuneScriptRunner.execute(state))
            println(result)
        }*/
    }

    private val dir = File("./data/scripts_bin/").toPath()


    private val scriptLookup = HashMap<Int, ScriptFile>()

    private val scriptNames = HashMap<String, Int>()

    var scripts: Array<ScriptFile?> = emptyArray()

    @JvmStatic
    fun parse(): Int {
        val start = System.currentTimeMillis()
        Logger.messageColor = Logger.Color.PURPLE
        Logger.info("Engine","---Lost-City (377)---")
        Logger.messageColor = Logger.Color.PURPLE
        Logger.info("Engine","......Compiling RuneScript......")
        ServerScriptCompilerCLI.main(emptyArray())

        val datPath = dir.resolve("script.dat")
        val idxPath = dir.resolve("script.idx")

        if (!datPath.toFile().exists() || !idxPath.toFile().exists()) {
            throw IllegalArgumentException("Both script.dat and script.idx must exist in $dir")
        }

        val dat = Packet.load(datPath.toFile())
        val idx = Packet.load(idxPath.toFile())

        if (dat.length() < 1 || idx.length() < 1) {
            throw IllegalArgumentException("Invalid RuneScript Blob")
        }

        val entries = dat.g4s()

        idx.move(4)

        val version = dat.g4s()

        if (version != 25) {
            throw IllegalArgumentException("Invalid RuneScript Compiler version or corrupt script bundle, version: $version")
        }

        var loaded = 0

        scripts = arrayOfNulls<ScriptFile?>(entries)

        for (id in 0 until entries) {
            val size = idx.g4s()
            if (size == 0) {
                continue
            }

            val data = ByteArray(size)
            dat.gdata(data, 0, size)
            val script = ScriptFile.decode(id, Packet(data))
            scripts[id] = script
            scriptNames[script.name()] = id

            if (script.info!!.lookupKey.toLong() != 0xffffffff) {
                scriptLookup[script.info!!.lookupKey] = script
            }

            loaded++
        }

        Logger.messageColor = Logger.Color.PURPLE
        Logger.info("Engine","Lost-City (377) Engine loaded in ${System.currentTimeMillis() - start}ms")
        Logger.messageColor = Logger.Color.PURPLE
        Logger.info("Engine","--------------------------------")
        return loaded
    }

    /**
     * Used to look up a script by the `type` and `category`.
     *
     * This function will attempt to search for a script given the specific `type`,
     * if one is not found it attempts one for `category`, and if still not found
     * it will attempt for the global script.
     *
     * @param trigger The script trigger to find.
     * @param type The script subject type id.
     * @param category The script subject category id.
     */
    fun getByTrigger(trigger: Int, type: Int = -1, category: Int = -1): ScriptFile? {
        scriptLookup[trigger or (0x2 shl 8) or (type shl 10)]?.let { return it }
        scriptLookup[trigger or (0x1 shl 8) or (category shl 10)]?.let { return it }
        return scriptLookup[trigger]
    }

    /**
     * Used to look up a script by a specific combo. Does not attempt any other combinations.
     *
     * If `type` is not `-1`, only the `type` specific script will be looked up. Likewise
     * for `category`. If both `type` and `category` are `-1`, then only the global script
     * will be looked up.
     *
     * @param trigger The script trigger to find.
     * @param type The script subject type id.
     * @param category The script subject category id.
     */
    fun getByTriggerSpecific(trigger: Int, type: Int = -1, category: Int = -1): ScriptFile? {
        if (type != -1) {
            return scriptLookup[trigger or (0x2 shl 8) or (type shl 10)]
        } else if (category != -1) {
            return scriptLookup[trigger or (0x1 shl 8) or (category shl 10)]
        }
        return scriptLookup[trigger]
    }

    /**
     * TESTING
     */
    fun get(id: Int): ScriptFile? {
        if (scripts.isEmpty())
            return FakeScriptFile.simple(opcodes = intArrayOf(-1))
        return scripts[id]
    }
}