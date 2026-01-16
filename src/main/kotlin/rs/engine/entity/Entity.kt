package rs.engine.entity

import rs.engine.World
import rs.util.Linkable

open class Entity(
    var level: Int,
    var x: Int,
    var z: Int,
    var width: Int,
    var length: Int,
    var lifecycle: Int,
    var isActive: Boolean = false
) : Linkable() {
    var lifecycleTick = -1
    var lastlifecycleTick = -1

    open fun resetEntity(respawn: Boolean) {
        TODO()
    }

    fun isValid(): Boolean {
        return isActive
    }

    fun setLifeCycle(tick: Int) {
        lifecycleTick = tick
        lastlifecycleTick = World.currentTick
    }
}