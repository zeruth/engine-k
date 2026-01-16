package rs.cache.config

import rs.io.Packet

object Parameters {
    fun decode(dat: Packet): HashMap<Int, Any> {
        val count = dat.g1()
        val params = HashMap<Int, Any>()

        repeat(count) {
            val key = dat.g3()
            val isString = dat.gbool()

            if (isString) {
                params[key] = dat.gjstr()
            } else {
                params[key] = dat.g4s()
            }
        }

        return params
    }
}
