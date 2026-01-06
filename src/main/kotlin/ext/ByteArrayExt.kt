package ext

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteArrayExt {
    fun ByteArray.buffer(order: ByteOrder = ByteOrder.LITTLE_ENDIAN): ByteBuffer {
        return ByteBuffer.wrap(this).order(order)
    }
}