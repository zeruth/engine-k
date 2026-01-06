package ext

import java.nio.ByteBuffer

object ByteBufferExt {
    /** Reads an unsigned 8-bit integer from the ByteBuffer and returns it as Int (0..255) */
    fun ByteBuffer.getUInt8(): Int {
        return get().toInt() and 0xFF
    }
}