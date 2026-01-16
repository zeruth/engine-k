package rs.engine.entity

open class PathingEntity(
    level: Int,
    x: Int,
    z: Int,
    width: Int,
    length: Int,
    lifecycle: Int,
    val moveRestrict: Int,
    val blockWalk: Int,
    val moveStrategy: MoveStrategy,
    val coordmask: Int,
    val entitymask: Int) : Entity(level, x, z, width, length, lifecycle) {

    init {

    }
}