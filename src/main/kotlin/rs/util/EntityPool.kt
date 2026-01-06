package rs.util

import rs.engine.entity.Entity

class EntityPool<T : Entity> {
    private val entities = mutableMapOf<Int, T>()

    val count: Int
        get() = entities.size

    fun add(id: Int, entity: T) {
        if (entities.containsKey(id)) throw IllegalStateException("ID already used")
        entities[id] = entity
    }

    fun remove(id: Int) {
        entities.remove(id)
    }

    operator fun get(id: Int) = entities[id]

    fun nextFreeId(): Int = (0..Int.MAX_VALUE).first { it !in entities }

    fun all(): Collection<T> = entities.values
}
