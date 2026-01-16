package rs.cache.config

import rs.cache.ConfigType
import rs.io.Packet
import util.Logger
import java.io.File

class StructType(id: Int) : ConfigType(id){
    companion object {
        private val dir = File("./data/pack/server/").toPath()

        private var configNames = HashMap<String, Int>()
        private var configs: Array<StructType?> = emptyArray()

        init {
            val dat = Packet.load(dir.resolve("struct.dat").toFile())
            parse(dat)
        }

        fun parse(dat: Packet) {
            val count = dat.g2()
            configs = arrayOfNulls(count)

            for (id in 0 until count) {
                val config = StructType(id)
                config.decodeType(dat)

                configs[id] = config

                if (config.debugname != null) {
                    configNames[config.debugname!!] = id
                }
            }

            Logger.messageColor = Logger.Color.GREEN
            Logger.info("Cache", "Loaded $count StructTypes")
        }

        fun get(id: Int) = configs[id]

        fun getId(name: String) = configNames[name]

        fun getByName(name: String): StructType? {
            val id = getId(name) ?: return null
            return get(id)
        }

        fun count() : Int {
            return configs.size
        }
    }

    var params = HashMap<Int, Any>()

    override fun decode(code: Int, dat: Packet) {
        when (code) {
            249 -> params = Parameters.decode(dat)
            250 -> debugname = dat.gjstr()
            else -> throw RuntimeException("Unhandled code $code")
        }
    }
}