package rs.util

import rs.engine.entity.Entity

class EntityPool<T : Entity>(private val maxSize: Int) : HashMap<Int, T>() {

    fun add(id: Int, entity: T) {
        if (containsKey(id)) throw IllegalStateException("ID already used")
        if (size >= maxSize) throw IllegalStateException("EntityPool is full (max size: $maxSize)")
        this[id] = entity
    }

    fun nextFreeId(): Int? = (0..Int.MAX_VALUE).firstOrNull { it !in this }
}
